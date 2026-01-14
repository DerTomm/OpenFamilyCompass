package org.openfamilycompass.init;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("initial-data")
@Slf4j
public class DataInitializerFromDump {

    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    @Value("classpath:data/initial-data.sql")
    private Resource initialDataScript;

    public DataInitializerFromDump(JdbcTemplate jdbcTemplate, Flyway flyway) {
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
        log.info("DataInitializerFromDump bean created");
    }

    @PostConstruct
    public void loadInitialData() {
        log.info("Resetting database with Flyway clean and migrate...");

        // Clean the database completely
        flyway.clean();

        // Repair to update checksums after clean
        flyway.repair();

        // Re-run all migrations to recreate schema
        flyway.migrate();

        log.info("Database reset complete. Loading initial data...");

        try {
            String sql = new String(FileCopyUtils.copyToByteArray(initialDataScript.getInputStream()),
                    StandardCharsets.UTF_8);
            // Split by semicolon and execute each statement
            String[] statements = sql.split(";");
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty()) {
                    jdbcTemplate.execute(statement);
                }
            }
            log.info("Initial data loaded successfully.");
        } catch (IOException e) {
            log.error("Failed to load initial data: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}