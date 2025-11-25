package org.example.infra;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.io.BufferedReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.example.repo.AlbumRepository;
import org.example.repo.ArtistRepository;
import org.example.repo.InvoiceRepository;
import org.example.repo.TrackRepository;


public class CsvLoader {

    /** Carga el CSV, imprime resumen y lista de inválidas (nada inválido persiste: ni parse/obligatorios ni DDL) */
    public void load(EntityManager em, Reader reader, boolean hasHeader, char separator) throws Exception {
        CsvLoadResult result = loadWithValidation(em, reader, hasHeader, separator);
        System.out.println("[CSV] " + result);
        if (result.getInvalidLines() > 0) {
            System.out.println("[CSV] Líneas inválidas encontradas (según restricciones del DDL):");
            result.getInvalidLineDetails().forEach(System.out::println);
        }
    }

    /** Carga con validación “pura” vs DDL; filas inválidas o que violan DDL NO se persisten */
    public CsvLoadResult loadWithValidation(EntityManager em, Reader reader, boolean hasHeader, char separator) throws Exception {
        CsvLoadResult result = new CsvLoadResult();

        try (BufferedReader br = new BufferedReader(reader)) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();

            //DesignerRepository designerRepo = new DesignerRepository(em);
            AlbumRepository albumRepo = new AlbumRepository(em);
            ArtistRepository artistRepo = new ArtistRepository(em);
            InvoiceRepository invoiceRepo = new InvoiceRepository(em);
            TrackRepository trackRepo = new TrackRepository(em);

            String line;
            int lineNumber = 0;

            if (hasHeader) { br.readLine(); lineNumber = 1; }

            int processed = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line == null || line.isBlank()) continue;

                List<String> cols = split(line, separator);

                // 1) columnas mínimas
                if (cols.size() < 10) {  // si son mas de 10, se cambia
                    result.addInvalidLine(lineNumber, "Columnas insuficientes (esperadas=10, recibidas=" + cols.size() + ")", line);
                    continue;
                }

                // Tokens crudos (para mensajes)
                // Tokens crudos (para mensajes)
                String nameRaw = nRaw(cols, 0);  // name sigue en la misma columna
                String categoryRaw = nRaw(cols, 1);  // category
                String yearRaw = nRaw(cols, 2);  // year_published
                String designerRaw = nRaw(cols, 3);  // designer
                String minAgeRaw = nRaw(cols, 4);  // min_age
                String avgRaw = nRaw(cols, 5);  // average_rating
                String usersRaw = nRaw(cols, 6);  // users_rated
                String minPRaw = nRaw(cols, 7);  // min_players
                String maxPRaw = nRaw(cols, 8);  // max_players
                String publisherRaw = nRaw(cols, 9);  // publisher

                // Valores normalizados (trim + parse)
                String name = n(cols, 0);  // name
                String categoryName = n(cols, 1);  // category (new position)
                Integer total = toInt(n(cols, 2));  // year_published (new position)
                String designerName = n(cols, 3);  // designer (new position)
                Integer minAge = toInt(n(cols, 4));  // min_age (new position)
                BigDecimal averageRating = toDec(n(cols, 5));  // average_rating (new position)
                Integer usersRating = toInt(n(cols, 6));  // users_rated (new position)
                Integer minPlayers = toInt(n(cols, 7));  // min_players (new position)
                Integer maxPlayers = toInt(n(cols, 8));  // max_players (new position)
                String publisherName = n(cols, 9);  // publisher (new position)


                // 2) obligatorios
                List<String> fatal = new ArrayList<>();
                if (name == null) fatal.add("NAME vacío");
                if (designerName == null) fatal.add("DESIGNER vacío");
                if (publisherName == null) fatal.add("PUBLISHER vacío");
                if (categoryName == null) fatal.add("CATEGORY vacío");

                // 3) diagnóstico de parseo (para campos opcionales)
                List<String> parseErrs = new ArrayList<>();
                if (hasText(yearRaw) && total == null) parseErrs.add("YEAR_PUBLISHED inválido: '" + yearRaw + "'");
                if (hasText(minAgeRaw) && minAge == null) parseErrs.add("MIN_AGE inválido: '" + minAgeRaw + "'");
                if (hasText(avgRaw) && averageRating == null) parseErrs.add("AVERAGE_RATING inválido: '" + avgRaw + "'");
                if (hasText(usersRaw) && usersRating == null) parseErrs.add("USERS_RATING inválido: '" + usersRaw + "'");
                if (hasText(minPRaw) && minPlayers == null) parseErrs.add("MIN_PLAYERS inválido: '" + minPRaw + "'");
                if (hasText(maxPRaw) && maxPlayers == null) parseErrs.add("MAX_PLAYERS inválido: '" + maxPRaw + "'");

                if (!fatal.isEmpty() || !parseErrs.isEmpty()) {
                    String reason = String.join(" | ", concat(fatal, parseErrs));
                    result.addInvalidLine(lineNumber, reason, line);
                    continue; // ← no persiste
                }

                // 4) VALIDACIÓN “pura” contra el DDL (ANTES de normalizar)
                List<String> ddlViolations = new ArrayList<>();

                // CK_YEAR_PUBLISHED (rango razonable)
                /*Integer y = yearPublished;
                if (y != null) {
                    int current = java.time.Year.now().getValue();
                    int upper = Math.min(current, 2100);
                    if (y < 1800 || y > upper) {
                        ddlViolations.add("CK_YEAR_PUBLISHED (fuera de rango: " + y + ")");
                    }
                }*/

                // CK_PLAYERS_RANGE (ambos null) OR (ambos no null y min>0 y max>=min)
                Integer minP = minPlayers, maxP = maxPlayers;
                boolean playersOk =
                        (minP == null && maxP == null)
                                || (minP != null && maxP != null && minP > 0 && maxP >= minP);
                if (!playersOk) {
                    ddlViolations.add("CK_PLAYERS_RANGE (min=" + minP + ", max=" + maxP + ")");
                }

                // CK_RATING_RANGE (0..10)
                if (averageRating != null) {
                    if (averageRating.compareTo(new BigDecimal("0")) < 0
                            || averageRating.compareTo(new BigDecimal("10")) > 0) {
                        ddlViolations.add("CK_RATING_RANGE (avg=" + averageRating + ")");
                    }
                }

                // CK_MIN_AGE_NONNEG (>=0)
                if (minAge != null && minAge < 0) {
                    ddlViolations.add("CK_MIN_AGE_NONNEG (minAge=" + minAge + ")");
                }

                if (!ddlViolations.isEmpty()) {
                    // Registrar e impedir persistencia de esta fila
                    result.addInvalidLine(lineNumber, String.join(" | ", ddlViolations), line);
                    continue; // ← clave: NO normalizar ni persistir
                }

                // 5) (Opcional) NORMALIZACIÓN SUAVE si quisieras mantenerla para casos límite

                // (ya no afecta a inválidas, porque las cortamos antes)
                // YEAR_PUBLISHED fuera de rango → NULL (ya no ocurrirá por el continue)
                // Players
                if (minPlayers != null && maxPlayers == null) maxPlayers = minPlayers;
                if (minPlayers == null && maxPlayers != null) minPlayers = Math.max(1, maxPlayers);
                if (minPlayers != null && minPlayers <= 0) minPlayers = 1;
                if (minPlayers != null && maxPlayers != null && maxPlayers < minPlayers) maxPlayers = minPlayers;
                // Rating
                if (averageRating != null) {
                    if (averageRating.compareTo(new BigDecimal("0")) < 0
                            || averageRating.compareTo(new BigDecimal("10")) > 0) {
                        averageRating = null;
                    }
                }
                // Min age
                if (minAge != null && minAge < 0) minAge = 0;

                // 6) Persistencia (solo filas válidas)
                // Nota: los repositorios getOrCreate ya hacen persist si es nuevo
                try {

                    /*{
                        Album album = albumRepo.getOrCreate(designerName); // cambiar según corresponda
                        Artist artist = artistRepo.getOrCreate(categoryName);
                        Invoice invoice = invoiceRepo.getOrCreate(yearPublished);
                        Track track = trackRepo.getOrCreate(categoryName);
                    }*/
                    /* gameRepo.getOrCreate( //eeste caso era una clase que dependia de otras entidades, en este caso los nombres de (designer, publisher, category)
                            name, yearPublished, minAge, averageRating, usersRating,
                            minPlayers, maxPlayers, album, artist, invoice, track); */

                    result.incrementValidLines();
                    processed++;
                    if (processed % 500 == 0) {
                        em.flush();
                        em.clear();
                    }
                } catch (Exception ex) {
                    result.addInvalidLine(lineNumber,
                            "Error al persistir: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                            line);
                }
            }

            result.setTotalLines(lineNumber);
            tx.commit();
            System.out.println("[SUMMARY] Filas OK: " + result.getValidLines()
                    + " | Filas inválidas: " + result.getInvalidLines()
                    + " | Total: " + result.getTotalLines());
        }

        return result;
    }

    // ===== Helpers =====

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    /** Token crudo (sin trim) para mensajes */
    private static String nRaw(List<String> cols, int i) {
        if (i >= cols.size()) return null;
        return cols.get(i);
    }

    /** Token normalizado (trim y ""→null) */
    private static String n(List<String> cols, int i) {
        if (i >= cols.size()) return null;
        String v = cols.get(i);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer toInt(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        s = s.replace(',', '.');
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        try { return Integer.valueOf(s); }
        catch (Exception e) {
            try { return (int) Math.round(Double.parseDouble(s)); }
            catch (Exception ex) { return null; }
        }
    }

    private static BigDecimal toDec(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        s = s.replace(',', '.');
        try { return new BigDecimal(s); }
        catch (Exception e) { return null; }
    }

    private static List<String> split(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == sep && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
