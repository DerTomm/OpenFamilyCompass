package org.openfamilycompass.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for exporting and importing database backups.
 * Exports all tables as SQL statements into a ZIP archive.
 * Stores metadata (migration level, timestamp, etc.) for compatibility checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final String PENDING_RESTORE_DIR = "pending-restore";
    private static final String PENDING_RESTORE_FILE = "backup-to-restore.zip";

    /**
     * Create a database backup as a ZIP file.
     * Returns byte array containing the ZIP file.
     */
    @Transactional
    public byte[] createBackup(String description) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            // Get current migration level
            int currentMigrationLevel = getCurrentMigrationLevel();

            // Create metadata
            BackupMetadata metadata = BackupMetadata.createCurrent(currentMigrationLevel, description);
            String metadataJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(metadata);

            // Add metadata to ZIP
            ZipEntry metadataEntry = new ZipEntry("metadata.json");
            zipOut.putNextEntry(metadataEntry);
            zipOut.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();

            // Export all tables
            try (Connection conn = dataSource.getConnection()) {
                List<String> tableNames = getAllTableNames(conn);

                for (String tableName : tableNames) {
                    log.info("Exporting table: {}", tableName);
                    String sqlDump = exportTableAsSQL(conn, tableName);

                    ZipEntry tableEntry = new ZipEntry("tables/" + tableName + ".sql");
                    zipOut.putNextEntry(tableEntry);
                    zipOut.write(sqlDump.getBytes(StandardCharsets.UTF_8));
                    zipOut.closeEntry();
                }
            }

            zipOut.finish();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error creating backup", e);
            throw new BackupException("Failed to create backup: " + e.getMessage(), e);
        }
    }

    /**
     * Validates and schedules a database backup for restoration on next server
     * restart.
     * The backup will be applied when the server starts up again, ensuring database
     * consistency and preventing runtime modifications.
     * 
     * @param zipData The backup ZIP file data
     * @throws BackupException if validation fails or file cannot be saved
     */
    public void restoreBackup(byte[] zipData) {
        try {
            // Validate the backup first
            BackupMetadata metadata = validateBackup(zipData);

            log.info("Backup validation successful. Migration level: {}, Description: {}",
                    metadata.getMigrationLevel(), metadata.getDescription());

            // Create pending restore directory
            Path pendingDir = Paths.get(PENDING_RESTORE_DIR);
            if (!Files.exists(pendingDir)) {
                Files.createDirectories(pendingDir);
                log.info("Created pending restore directory: {}", pendingDir.toAbsolutePath());
            }

            // Save backup file for next server restart
            Path backupFile = pendingDir.resolve(PENDING_RESTORE_FILE);
            Files.write(backupFile, zipData);

            log.info("Backup scheduled for restoration on next server restart: {}", backupFile.toAbsolutePath());
            log.warn(
                    "IMPORTANT: Server must be restarted to apply the backup. No changes have been made to the running database.");

        } catch (BackupException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error saving backup for scheduled restore", e);
            throw new BackupException("Failed to schedule backup restore: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a backup file without applying it.
     * Checks metadata and compatibility with current migration level.
     * 
     * @param zipData The backup ZIP file data
     * @return BackupMetadata if validation succeeds
     * @throws BackupException if validation fails
     */
    private BackupMetadata validateBackup(byte[] zipData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
                ZipInputStream zipIn = new ZipInputStream(bais)) {

            byte[] buffer = new byte[1024];
            BackupMetadata metadata = null;
            ByteArrayOutputStream metadataBuffer = new ByteArrayOutputStream();

            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entry.getName().equals("metadata.json")) {
                    int len;
                    while ((len = zipIn.read(buffer)) > 0) {
                        metadataBuffer.write(buffer, 0, len);
                    }
                    metadata = objectMapper.readValue(
                            metadataBuffer.toByteArray(), BackupMetadata.class);
                    break;
                }
            }

            if (metadata == null) {
                throw new BackupException("Backup archive does not contain metadata.json");
            }

            // Verify compatibility
            int currentMigrationLevel = getCurrentMigrationLevel();
            if (!metadata.isCompatibleWith(currentMigrationLevel)) {
                throw new BackupException(
                        metadata.getCompatibilityMessage(currentMigrationLevel));
            }

            return metadata;

        } catch (BackupException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating backup", e);
            throw new BackupException("Failed to validate backup: " + e.getMessage(), e);
        }
    }

    /**
     * Checks for and applies any pending backup restoration.
     * This method is called during server startup.
     * If a pending restore is found, it will be applied and then removed.
     */
    @Transactional
    public void applyPendingRestore() {
        Path pendingDir = Paths.get(PENDING_RESTORE_DIR);
        Path backupFile = pendingDir.resolve(PENDING_RESTORE_FILE);

        if (!Files.exists(backupFile)) {
            log.debug("No pending backup restore found");
            return;
        }

        log.info("Pending backup restore found: {}", backupFile.toAbsolutePath());
        log.warn("Starting database restoration from backup. This may take several minutes...");

        try {
            byte[] zipData = Files.readAllBytes(backupFile);

            // Re-validate before applying
            BackupMetadata metadata = validateBackup(zipData);
            log.info("Re-validated backup. Migration level: {}", metadata.getMigrationLevel());

            // Apply the restore
            performRestore(zipData);

            // Delete the pending restore file
            Files.delete(backupFile);
            log.info("Backup restore completed successfully and pending restore file removed");

        } catch (Exception e) {
            log.error("CRITICAL: Failed to apply pending backup restore", e);
            // Rename the failed backup file for investigation
            try {
                Path failedFile = pendingDir.resolve("failed-restore-" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip");
                Files.move(backupFile, failedFile, StandardCopyOption.REPLACE_EXISTING);
                log.error("Failed backup moved to: {}", failedFile.toAbsolutePath());
            } catch (IOException moveEx) {
                log.error("Could not move failed backup file", moveEx);
            }
            throw new BackupException("Failed to apply pending restore: " + e.getMessage(), e);
        }
    }

    /**
     * Performs the actual database restore operation.
     * This is called during server startup when a pending restore is found.
     * 
     * @param zipData The validated backup ZIP file data
     */
    @Transactional
    private void performRestore(byte[] zipData) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
                ZipInputStream zipIn = new ZipInputStream(bais)) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];

            // Restore tables
            try (Connection conn = dataSource.getConnection()) {
                // Disable foreign key constraints for import
                disableForeignKeyConstraints(conn);

                try {
                    // Clear all existing data
                    clearAllTables(conn);

                    // Import tables from backup
                    try (ZipInputStream zipIn3 = new ZipInputStream(new ByteArrayInputStream(zipData))) {
                        while ((entry = zipIn3.getNextEntry()) != null) {
                            if (entry.getName().startsWith("tables/") && entry.getName().endsWith(".sql")) {
                                ByteArrayOutputStream sqlBuffer = new ByteArrayOutputStream();
                                int len;
                                while ((len = zipIn3.read(buffer)) > 0) {
                                    sqlBuffer.write(buffer, 0, len);
                                }

                                String sql = sqlBuffer.toString(StandardCharsets.UTF_8);
                                importTableFromSQL(conn, sql);
                                log.info("Restored table: {}", entry.getName());
                            }
                        }
                    }
                } finally {
                    // Re-enable foreign key constraints
                    enableForeignKeyConstraints(conn);
                }
            }
        }
    }

    /**
     * Get all table names from the database (excluding system/migration tables).
     */
    private List<String> getAllTableNames(Connection conn) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet tables = meta.getTables(null, "public", "%", new String[] { "TABLE" })) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                // Exclude Flyway migration tables
                if (!tableName.startsWith("flyway")) {
                    tableNames.add(tableName);
                }
            }
        }

        return tableNames;
    }

    /**
     * Export a table as SQL INSERT statements.
     */
    private String exportTableAsSQL(Connection conn, String tableName) throws SQLException {
        StringBuilder sql = new StringBuilder();

        // Get column names and types
        DatabaseMetaData meta = conn.getMetaData();
        List<String> columnNames = new ArrayList<>();
        List<String> columnTypes = new ArrayList<>();

        try (ResultSet columns = meta.getColumns(null, "public", tableName, null)) {
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"));
                columnTypes.add(columns.getString("TYPE_NAME"));
            }
        }

        if (columnNames.isEmpty()) {
            return "";
        }

        // Add header comment
        sql.append("-- Table: ").append(tableName).append("\n");
        sql.append("-- Exported: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        // Build SELECT query
        StringBuilder selectCols = new StringBuilder();
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0)
                selectCols.append(", ");
            selectCols.append("\"").append(columnNames.get(i)).append("\"");
        }

        String selectSql = "SELECT " + selectCols + " FROM \"" + tableName + "\"";

        // Execute and export rows
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(selectSql)) {

            StringBuilder cols = new StringBuilder();
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0)
                    cols.append(", ");
                cols.append("\"").append(columnNames.get(i)).append("\"");
            }

            while (rs.next()) {
                sql.append("INSERT INTO \"").append(tableName).append("\" (")
                        .append(cols).append(") VALUES (");

                for (int i = 1; i <= columnNames.size(); i++) {
                    if (i > 1)
                        sql.append(", ");

                    String columnType = columnTypes.get(i - 1).toLowerCase();

                    // Handle bytea columns explicitly using getBytes()
                    if (columnType.equals("bytea")) {
                        byte[] bytes = rs.getBytes(i);
                        if (bytes == null) {
                            sql.append("NULL");
                        } else {
                            sql.append("E'\\\\x");
                            for (byte b : bytes) {
                                sql.append(String.format("%02x", b));
                            }
                            sql.append("'");
                        }
                        continue;
                    }

                    Object value = rs.getObject(i);

                    if (value == null) {
                        sql.append("NULL");
                    } else if (value instanceof byte[]) {
                        // Handle byte arrays
                        byte[] bytes = (byte[]) value;
                        sql.append("E'\\\\x");
                        for (byte b : bytes) {
                            sql.append(String.format("%02x", b));
                        }
                        sql.append("'");
                    } else if (value instanceof java.sql.Array) {
                        // Handle SQL Array type
                        java.sql.Array array = (java.sql.Array) value;
                        String arrayStr = array.toString();
                        sql.append("'").append(escapeSQL(arrayStr)).append("'");
                    } else if (value instanceof String) {
                        sql.append("'").append(escapeSQL((String) value)).append("'");
                    } else if (value instanceof java.sql.Timestamp) {
                        sql.append("'").append(value.toString()).append("'");
                    } else if (value instanceof java.sql.Date) {
                        sql.append("'").append(value.toString()).append("'");
                    } else if (value instanceof Boolean) {
                        sql.append((Boolean) value ? "true" : "false");
                    } else if (value instanceof Number) {
                        // Handle all numeric types directly
                        sql.append(value);
                    } else {
                        // For all other types, convert to string and escape
                        String strValue = value.toString();
                        // Check if it looks like a Java object reference [B@... or similar
                        if (strValue.matches("^\\[.*@[a-f0-9]+$")) {
                            log.warn("Skipping malformed data in table {} column {}: {}",
                                    tableName, columnNames.get(i - 1), strValue);
                            sql.append("NULL");
                        } else {
                            sql.append("'").append(escapeSQL(strValue)).append("'");
                        }
                    }
                }

                sql.append(");\n");
            }
        }

        return sql.toString();
    }

    /**
     * Import table data from SQL statements.
     */
    private void importTableFromSQL(Connection conn, String sql) throws SQLException {
        if (sql.trim().isEmpty()) {
            return;
        }

        // Split by semicolon and execute each statement
        String[] statements = sql.split(";(?=(?:[^']*'[^']*')*[^']*$)"); // Split on ; outside quotes

        try (Statement stmt = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    /**
     * Clear all data from tables (preserves schema).
     */
    private void clearAllTables(Connection conn) throws SQLException {
        List<String> tableNames = getAllTableNames(conn);

        try (Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                String truncateSql = "TRUNCATE TABLE \"" + tableName + "\" CASCADE";
                log.info("Clearing table: {}", tableName);
                stmt.execute(truncateSql);
            }
        }
    }

    /**
     * Disable foreign key constraints in PostgreSQL.
     */
    private void disableForeignKeyConstraints(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Disable all triggers (which includes foreign key checks) for this session
            stmt.execute("SET session_replication_role = 'replica'");
            log.debug("Foreign key constraints disabled for restore");
        }
    }

    /**
     * Re-enable foreign key constraints in PostgreSQL.
     */
    private void enableForeignKeyConstraints(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Re-enable all triggers
            stmt.execute("SET session_replication_role = 'origin'");
            log.debug("Foreign key constraints re-enabled");
        }
    }

    /**
     * Get current migration level from Flyway migration history table.
     * Queries the flyway_schema_history table directly.
     */
    private int getCurrentMigrationLevel() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Query the flyway_schema_history table for the latest migration
            String query = "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1";
            try (ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    String version = rs.getString("version");
                    // Extract version number (e.g., "4" from "4")
                    try {
                        return Integer.parseInt(version.split("\\.")[0]);
                    } catch (Exception e) {
                        log.warn("Could not parse migration version: {}", version);
                        return 0;
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("Could not retrieve migration level from database", e);
            return 0;
        }
        return 0;
    }

    /**
     * Escape single quotes in SQL strings.
     */
    private String escapeSQL(String str) {
        return str.replace("'", "''");
    }
}
