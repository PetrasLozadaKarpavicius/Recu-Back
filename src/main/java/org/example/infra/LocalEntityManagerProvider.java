package org.example.infra;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Proveedor de EntityManagerFactory configurado para usar el DataSource centralizado.
 * hibernate.hbm2ddl.auto = none porque el DDL lo ejecuta DbInitializer.
 */
public final class LocalEntityManagerProvider {

    private static final String PERSISTENCE_UNIT = "database";
    private static volatile EntityManagerFactory emf;

    private LocalEntityManagerProvider() {}

    public static EntityManagerFactory get() {
        if (emf == null) {
            synchronized (LocalEntityManagerProvider.class) {
                if (emf == null) {
                    DataSource ds = DataSourceProvider.get();
                    Map<String, Object> props = new HashMap<>();
                    props.put("hibernate.hbm2ddl.auto", "none");
                    props.put("hibernate.show_sql", "true");
                    props.put("hibernate.format_sql", "true");
                    // Pasa el DataSource directamente a Hibernate
                    props.put("hibernate.connection.datasource", ds);
                    // Dialecto explícito para H2 (opcional pero recomendable)
                    props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

                    emf = new HibernatePersistenceProvider()
                            .createEntityManagerFactory(PERSISTENCE_UNIT, props);
                }
            }
        }
        return emf;
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
    }

}

