package org.openfamilycompass.backup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for database backups.
 * Stored in metadata.json inside the backup archive.
 * Used to verify compatibility when restoring.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupMetadata {

    @JsonProperty("backup_timestamp")
    private String backupTimestamp;

    @JsonProperty("migration_level")
    private int migrationLevel;

    @JsonProperty("application_version")
    private String applicationVersion;

    @JsonProperty("database_type")
    private String databaseType;

    @JsonProperty("backup_format_version")
    private int backupFormatVersion = 1; // For future compatibility

    @JsonProperty("description")
    private String description;

    /**
     * Create metadata for current backup
     */
    public static BackupMetadata createCurrent(int migrationLevel, String description) {
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.setMigrationLevel(migrationLevel);
        metadata.setApplicationVersion("1.0.0-SNAPSHOT");
        metadata.setDatabaseType("PostgreSQL");
        metadata.setDescription(description != null ? description : "Database backup");
        return metadata;
    }

    /**
     * Check if this backup is compatible with the current migration level.
     * Currently, we require exact match, but can be extended for
     * upgrades/downgrades.
     */
    public boolean isCompatibleWith(int currentMigrationLevel) {
        // For now: exact match required
        return this.migrationLevel == currentMigrationLevel;
    }

    /**
     * Get compatibility message
     */
    public String getCompatibilityMessage(int currentMigrationLevel) {
        if (isCompatibleWith(currentMigrationLevel)) {
            return "Backup is compatible with current migration level " + currentMigrationLevel;
        }
        return String.format("Backup level (%d) does not match current level (%d). Restore not possible.",
                this.migrationLevel, currentMigrationLevel);
    }
}
