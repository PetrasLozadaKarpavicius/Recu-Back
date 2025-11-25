package org.example.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.example.domain.*;
import org.example.repo.AlbumRepository;
import org.example.repo.ArtistRepository;
import org.example.repo.TrackRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para una fila CSV que representa un Track (Chinook-like).
 * - parsea campos crudos
 * - valida reglas simples (required, numeros, ranges)
 * - mapea/persiste entidades del dominio usando el EntityManager
 */
public class CsvTrackRecord_primVer {
    // Campos crudos (pueden venir en cualquier orden según tu CSV)
    private final String trackName;
    private final String albumTitle;
    private final String artistName;
    private final String mediaTypeName;
    private final String genreName;
    private final String composer;
    private final String millisecondsRaw;
    private final String bytesRaw;
    private final String unitPriceRaw;

    // Parsed
    private Integer milliseconds;
    private Integer bytes;
    private BigDecimal unitPrice;

    public CsvTrackRecord_primVer(String trackName, String albumTitle, String artistName,
                                  String mediaTypeName, String genreName, String composer,
                                  String millisecondsRaw, String bytesRaw, String unitPriceRaw) {
        this.trackName = trimToNull(trackName);
        this.albumTitle = trimToNull(albumTitle);
        this.artistName = trimToNull(artistName);
        this.mediaTypeName = trimToNull(mediaTypeName);
        this.genreName = trimToNull(genreName);
        this.composer = trimToNull(composer);
        this.millisecondsRaw = millisecondsRaw;
        this.bytesRaw = bytesRaw;
        this.unitPriceRaw = unitPriceRaw;
    }

    public static CsvTrackRecord_primVer fromColumns(List<String> cols) {
        // Ajustar índices según tu CSV real
        return new CsvTrackRecord_primVer(
                get(cols, 0), // track name
                get(cols, 1), // album title
                get(cols, 2), // artist name
                get(cols, 3), // media type
                get(cols, 4), // genre
                get(cols, 5), // composer
                get(cols, 6), // milliseconds
                get(cols, 7), // bytes
                get(cols, 8)  // unit price
        );
    }

    public List<String> validate() {
        List<String> errs = new ArrayList<>();

        if (trackName == null) errs.add("NAME vacío");
        if (albumTitle == null) errs.add("ALBUM vacío");
        if (artistName == null) errs.add("ARTIST vacío");

        // Parse numeric fields
        this.milliseconds = safeToInt(millisecondsRaw);
        this.bytes = safeToInt(bytesRaw);
        this.unitPrice = safeToDec(unitPriceRaw);

        if (millisecondsRaw != null && milliseconds == null)
            errs.add("MILLISECONDS inválido: '" + millisecondsRaw + "'");
        if (bytesRaw != null && bytes == null)
            errs.add("BYTES inválido: '" + bytesRaw + "'");
        if (unitPriceRaw != null && unitPrice == null)
            errs.add("UNIT_PRICE inválido: '" + unitPriceRaw + "'");

        // Reglas DDL / sentido
        if (milliseconds != null && milliseconds <= 0) errs.add("MILLISECONDS debe ser > 0");
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) <= 0) errs.add("UNIT_PRICE debe ser > 0");

        // Longitudes aproximadas (según DDL)
        if (trackName != null && trackName.length() > 200) errs.add("NAME demasiado largo (>200)");
        if (albumTitle != null && albumTitle.length() > 160) errs.add("ALBUM TITLE demasiado largo (>160)");
        if (artistName != null && artistName.length() > 120) errs.add("ARTIST NAME demasiado largo (>120)");

        return errs;
    }

    /**
     * Mapea y persiste (si no existen) las entidades necesarias y retorna el Track persistido.
     * No maneja la transacción (dejar al caller).
     */
    public Track toDomain(EntityManager em) {
        ArtistRepository artistRepo = new ArtistRepository(em);
        AlbumRepository albumRepo = new AlbumRepository(em);
        TrackRepository trackRepo = new TrackRepository(em);

        // Artist
        Artist artist = artistRepo.findbyName(artistName);
        if (artist == null) {
            artist = new Artist();
            artist.setName(artistName);
            em.persist(artist);
        }

        // Album (asegurar FK a artist)
        Album album = albumRepo.findByTitle(albumTitle);
        if (album == null) {
            album = new Album();
            album.setTitle(albumTitle);
            album.setArtistId(artist);
            em.persist(album);
        } else if (album.getArtistId() == null) {
            album.setArtistId(artist);
        }

        // MediaType (find or create)
        MediaType mediaType = findOrCreateMediaType(em, mediaTypeName);

        // Genre (find or create if provided)
        Genre genre = null;
        if (genreName != null) genre = findOrCreateGenre(em, genreName);

        // Track (si existe por nombre+album, devolver)
        Track existing = trackRepo.findbyName(trackName);
        if (existing != null && existing.getAlbum() != null &&
            existing.getAlbum().getTitle().equals(albumTitle)) {
            return existing;
        }

        Track t = new Track();
        t.setName(trackName);
        t.setAlbum(album);
        t.setMediaType(mediaType);
        t.setGenre(genre);
        t.setComposer(composer);
        t.setMilliseconds(this.milliseconds);
        t.setBytes(this.bytes);
        t.setUnitPrice(this.unitPrice);

        em.persist(t);
        return t;
    }

    // ----- helpers -----
    private static String get(List<String> cols, int i) {
        if (i >= cols.size()) return null;
        String v = cols.get(i);
        return v == null ? null : v.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer safeToInt(String s) {
        if (s == null) return null;
        try {
            s = s.trim().replace(',', '.');
            if (s.endsWith(".0")) s = s.substring(0, s.length()-2);
            return Integer.valueOf(s);
        } catch (Exception e) { return null; }
    }

    private static BigDecimal safeToDec(String s) {
        if (s == null) return null;
        try {
            s = s.trim().replace(',', '.');
            return new BigDecimal(s);
        } catch (Exception e) { return null; }
    }

    private static MediaType findOrCreateMediaType(EntityManager em, String name) { //crea si no existe el tipo de medio
        if (name == null) return null;
        TypedQuery<MediaType> q = em.createQuery("SELECT m FROM MediaType m WHERE m.name = :n", MediaType.class);
        q.setParameter("n", name);
        try { return q.getSingleResult(); } catch (Exception e) { /* not found */ }
        MediaType m = new MediaType();
        m.setName(name);
        em.persist(m);
        return m;
    }

    private static Genre findOrCreateGenre(EntityManager em, String name) { //crea si no existe el genero
        if (name == null) return null;
        TypedQuery<Genre> q = em.createQuery("SELECT g FROM Genre g WHERE g.name = :n", Genre.class);
        q.setParameter("n", name);
        try { return q.getSingleResult(); } catch (Exception e) { /* not found */ }
        Genre g = new Genre();
        g.setName(name);
        em.persist(g);
        return g;
    }
}