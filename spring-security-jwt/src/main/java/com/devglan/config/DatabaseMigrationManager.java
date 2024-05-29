package com.devglan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseMigrationManager {

    private final DataSource dataSource;

    @Autowired
    public DatabaseMigrationManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        applyScripts("schema.sql");
    }

    private void applyScripts(String scriptLocation) {
        try (Connection conn = dataSource.getConnection()) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(scriptLocation));
            populator.setContinueOnError(true); // This can be set based on your preference
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(scriptLocation));
        } catch (ScriptException e) {
            System.err.println("Error during schema initialization: " + e.getMessage());
            handleScriptExceptions(e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    private void handleScriptExceptions(ScriptException e) {
        // Custom handling based on exception types or specific errors
        if (e.getMessage().contains("Constraint already exists")) {
            System.out.println("Constraint already exists, skipping this part of the script.");
        } else {
            throw e; // Rethrow if it's an error that should not be ignored
        }
    }
}