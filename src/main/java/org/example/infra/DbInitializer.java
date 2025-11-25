package org.example.infra;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;


//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//public final class DbInitializer {
//
//    private static final String URL = "jdbc:h2:mem:database;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
//    private static final String USER = "sa";
//    private static final String PASSWORD = "";
//
//    private DbInitializer() {}
//
//    /**
//     * Inicializa la BD desde:
//     * - "classpath:sql/database-ddl.sql"
//     * - "file:src/main/resources/sql/database-ddl.sql"
//     * - "sql/database-ddl.sql" (intenta classpath primero, luego FS)
//     * - "C:/ruta/absoluta/database-ddl.sql"
//     */
//    public static void initialize(String ddlLocation) throws SQLException, IOException {
//        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
//             InputStream is = resolveInputStream(ddlLocation)) {
//
//            if (is == null) {
//                throw new FileNotFoundException("Resource not found (classpath or file): " + ddlLocation);
//            }
//
//            StringBuilder sb = new StringBuilder();
//            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = br.readLine()) != null) {
//                    sb.append(line).append('\n');
//                }
//            }
//
//            // Divide por ';' y ejecuta cada sentencia no vacía.
//            String[] statements = sb.toString().split(";");
//            conn.setAutoCommit(false);
//            try (Statement stmt = conn.createStatement()) {
//                for (String raw : statements) {
//                    String sql = raw.trim();
//                    if (sql.isEmpty()) continue;
//                    stmt.execute(sql);
//                }
//                conn.commit();
//            } catch (SQLException e) {
//                conn.rollback();
//                throw e;
//            }
//        }
//    }
//
//    /**
//     * Compatibilidad con la versión anterior que recibía solo el recurso en classpath.
//     */
//    public static void initializeFromClasspath(String ddlResource) throws SQLException, IOException {
//        // intenta como recurso del classpath (sin prefijo)
//        initialize("classpath:" + ddlResource);
//    }
//
//    private static InputStream resolveInputStream(String ddlLocation) throws IOException {
//        if (ddlLocation == null) return null;
//
//        // prefijo explícito classpath:
//        if (ddlLocation.startsWith("classpath:")) {
//            String res = ddlLocation.substring("classpath:".length());
//            return Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
//        }
//
//        // prefijo explícito file:
//        if (ddlLocation.startsWith("file:")) {
//            String path = ddlLocation.substring("file:".length());
//            return Files.newInputStream(Path.of(path));
//        }
//
//        // si no tiene prefijo, intenta classpath primero
//        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(ddlLocation);
//        if (is != null) return is;
//
//        // luego intenta como ruta de archivo en el filesystem (relativa o absoluta)
//        Path p = Path.of(ddlLocation);
//        if (Files.exists(p)) {
//            return Files.newInputStream(p);
//        }
//
//        return null;
//    }
//
//    public static void main(String[] args) {
//        try {
//            initializeFromClasspath("database-ddl.sql");
//            System.out.println("DDL ejecutado correctamente.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
//}
public class DbInitializer {
    public static void init() {
        try (Connection conn = DataSourceProvider.get().getConnection();
             Statement st = conn.createStatement()) {

            try (InputStream is = DbInitializer.class.getResourceAsStream("/sql/database-ddl.sql")) {
                if (is == null) throw new RuntimeException("DDL not found at /sql/database-ddl.sql");
                String script = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();

                // Remove all line comments starting with '--' (inline and full-line)
                String withoutLineComments = script.replaceAll("(?m)--.*$", "");
                // Optionally remove block comments /* ... */ if present
                String withoutBlockComments = withoutLineComments.replaceAll("/\\*([\\s\\S]*?)\\*/", "");

                for (String raw : withoutBlockComments.split(";")) {
                    String sql = raw.trim();
                    if (sql.isEmpty()) continue;
                    st.execute(sql);
                }
                System.out.println("[OK] DDL executed successfully");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing DDL", e);
        }
    }
}