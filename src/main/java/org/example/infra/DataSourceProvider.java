
package org.example.infra;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.Statement;

/**
 * Proveedor único de DataSource (H2 en memoria).
 */
public final class DataSourceProvider {


    private static DataSource dataSource;

    public static DataSource get() {
        if (dataSource == null) {
            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:database;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            dataSource = ds;
        }
        return dataSource;
    }
}
