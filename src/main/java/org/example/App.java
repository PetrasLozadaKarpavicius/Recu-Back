// java
package org.example;

import org.example.infra.CsvLoadResult;
import org.example.infra.CsvLoader;
import org.example.infra.DbInitializer;
import org.example.infra.LocalEntityManagerProvider;
import jakarta.persistence.EntityManager;




import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;


public class App {
    public static void main(String[] args) throws IOException {
        try {
            // 1) Inicializa BD (H2 en memoria) ejecutando el DDL
            DbInitializer.init();
            System.out.println("[OK] DDL ejecutado.");
            var emf = LocalEntityManagerProvider.get();
            try (EntityManager em = emf.createEntityManager()) {

                // 2) Carga CSV con validación y resumen (usa separador ';' o ',' según tu archivo)
                CsvLoadResult csvResult = null; // Renombramos 'result' a 'csvResult' para evitar la colisión
                var loader = new CsvLoader();
                try (var is = App.class.getResourceAsStream("/data/database.csv")) {
                    if (is == null) {
                        System.out.println("[WARN] No se encontró /data/boardgames.csv en resources");
                    } else {
                        var reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                        // hasHeader=true, ajustar separador según tu CSV
                        csvResult = loader.loadWithValidation(em, reader, true, ';');
                    }
                }// 3) Muestra resumen y lista de líneas inválidas (si hay)
                if (csvResult != null) {
                    System.out.println("RESULTADO PUNTO UNO: Resumen de importación");
                    System.out.println("[CSV] " + csvResult);
                    if (csvResult.getInvalidLines() > 0) {
                        System.out.println("[CSV] Líneas inválidas (según restricciones del DDL):");
                        csvResult.getInvalidLineDetails().forEach(System.out::println);
                    }
                }
                // Resolver PUNTOS DEL RECUPERATORIO A PARTIR DE AQUÍ

                // 4) Consultas para obtener cantidades

//                Long !!games = em.createQuery("select count(b) from BoardGame b", Long.class).getSingleResult();
//                Long designers = em.createQuery("select count(d) from Designer d", Long.class).getSingleResult();
//                Long publishers = em.createQuery("select count(p) from Publisher p", Long.class).getSingleResult();
//                Long categories = em.createQuery("select count(c) from Category c", Long.class).getSingleResult();

                // 5) Informe final de cantidad de registros

//                System.out.println("RESULTADO PUNTO UNO: Resumen de importación:");
//                System.out.println("Cantidad de juegos importados: " + !!games);
//                System.out.println("Cantidad de diseñadores creados: " + designers);
//                System.out.println("Cantidad de publishers creados: " + publishers);
//                System.out.println("Cantidad de categorías creadas: " + categories);

                // 6) Análisis de categorías con menor promedio de rating (>500 usuarios)

//                String jpql =
//                        "SELECT c.name, AVG(b.averageRating), SUM(b.usersRating) " +
//                                "FROM BoardGame b " +
//                                "JOIN b.category c " +
//                                "GROUP BY c.name " +
//                                "HAVING SUM(b.usersRating) > 500 " +
//                                "ORDER BY AVG(b.averageRating) ASC";
//
//                List<Object[]> categoryResults = em.createQuery(jpql, Object[].class)
//                        .setMaxResults(5)  // Limitar a las 5 categorías con menor promedio
//                        .getResultList();

                // Mostrar las 5 categorías con menor promedio de rating
//                System.out.println("RESULTADO PUNTO DOS: Ranking de peores categorías");
//                System.out.println("----------------------------------------------------------");
//
//                for (Object[] categoryResult : categoryResults) {
//                    String categoryName = (String) categoryResult[0];
//                    Double avgRating = (Double) categoryResult[1];
//                    Long usersRated = (Long) categoryResult[2];
//
//                    // Formato de salida
//                    System.out.printf("%-30s %.2f (%d usuarios)\n", categoryName, avgRating, usersRated);
//                }
//
//                // 7) Consultar juegos aptos para 4 jugadores y edad 4+
//                int players = 4;   // Número de jugadores
//                int age = 4;       // Edad mínima
//
//                // Consultamos todos los juegos
//                List<BoardGame> suitableGames = em.createQuery("SELECT b FROM BoardGame b", BoardGame.class)
//                        .getResultList();
//
//                // Filtramos por los juegos aptos para 4 jugadores y edad mínima 4
//                System.out.println("RESULTADO PUNTO TRES: Juegos aptos para 4 jugadores y edad 4+");
//                System.out.println("---------------------------------------------");
//
//                suitableGames.stream()
//                        .filter(bg -> bg.isSuitableForPlayersAndAge(players, age))  // Filtro por condiciones
//                        .sorted((bg1, bg2) -> bg2.getAverageRating().compareTo(bg1.getAverageRating()))  // Ordenar por rating
//                        .forEach(bg -> System.out.println(
//                                bg.getName() + " ............. Edad mínima: " + bg.getMinAge() +
//                                        " Jugadores: " + bg.getMinPlayers() + "-" + bg.getMaxPlayers()));
//            }
//
           }




        } catch (Exception e) {System.out.println("[FAIL] Error en inicialización/carga CSV");
            e.printStackTrace();
        }
    }
}
