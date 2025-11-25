// java
package org.example.infra;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DbInitializer_viejo {

    private static final String URL = "jdbc:h2:mem:database;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private DbInitializer_viejo() {}

    /**
     * Ejecuta el script DDL localizado en el classpath (por ejemplo, "database-ddl.sql").
     * Lanza IOException/SQLException en caso de fallo.
     */
    public static void initializeFromClasspath(String ddlResource) throws SQLException, IOException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(ddlResource)) {

            if (is == null) {
                throw new FileNotFoundException("Resource not found on classpath: " + ddlResource);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }

            // Divide por ';' y ejecuta cada sentencia no vacía.
            String[] statements = sb.toString().split(";");
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                for (String raw : statements) {
                    String sql = raw.trim();
                    if (sql.isEmpty()) continue;
                    stmt.execute(sql);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /*
    }*/
}