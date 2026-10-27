package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Component("filmDbStorage")
@Primary
@Slf4j
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbc;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final DirectorStorage directorStorage; // Добавлено для валидации

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"film_id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.longValue());
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        if (film.getDirectorIds() != null && !film.getDirectorIds().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectorIds());
        }

        log.debug("Фильм создан в БД: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE film_id = ?";
        int rows = jdbc.update(sql, film.getName(), film.getDescription(),
                java.sql.Date.valueOf(film.getReleaseDate()), film.getDuration(),
                film.getMpa().getId(), film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        jdbc.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        jdbc.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        if (film.getDirectorIds() != null && !film.getDirectorIds().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectorIds());
        }

        log.debug("Фильм обновлен в БД: {}", film);
        return film;
    }

    @Override
    public void delete(Long id) {
        jdbc.update("DELETE FROM film_genres WHERE film_id = ?", id);
        jdbc.update("DELETE FROM film_directors WHERE film_id = ?", id);
        String sql = "DELETE FROM films WHERE film_id = ?";
        int rows = jdbc.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        log.debug("Фильм удален из БД: id={}", id);
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films WHERE film_id = ?";
        List<Film> films = jdbc.query(sql, filmMapper, id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        enrichFilm(film);
        return Optional.of(film);
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films";
        List<Film> films = jdbc.query(sql, filmMapper);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getFilmsByDirectorId(Long directorId, String sortBy) {
        // ИСПРАВЛЕНО: удалён мёртвый код "return List.of();"
        if (directorStorage.findById(directorId).isEmpty()) {
            throw new NotFoundException("Режиссёр с id " + directorId + " не найден");
        }

        String orderBy = "year".equalsIgnoreCase(sortBy) ? "f.release_date ASC" : "COUNT(l.user_id) DESC";

        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films f " +
                "JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "ORDER BY " + orderBy;

        List<Film> films = jdbc.query(sql, filmMapper, directorId);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        boolean searchByTitle = by.contains("title");
        boolean searchByDirector = by.contains("director");

        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films f " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "WHERE (? = TRUE AND LOWER(f.name) LIKE ?) " +
                "   OR (? = TRUE AND (LOWER(d.name) LIKE ? OR LOWER(d.surname) LIKE ?)) " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "ORDER BY COUNT(l.user_id) DESC";

        List<Film> films = jdbc.query(sql, filmMapper,
                searchByTitle, searchPattern,
                searchByDirector, searchPattern, searchPattern);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films f " +
                "JOIN ( " +
                "    SELECT film_id FROM likes WHERE user_id = ? " +
                "    INTERSECT " +
                "    SELECT film_id FROM likes WHERE user_id = ? " +
                ") common ON f.film_id = common.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "ORDER BY COUNT(l.user_id) DESC";

        List<Film> films = jdbc.query(sql, filmMapper, userId, friendId);
        enrichFilms(films);
        return films;
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        genres.forEach(genre -> jdbc.update(sql, filmId, genre.getId()));
    }

    private void saveDirectors(Long filmId, Set<Long> directorIds) {
        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        directorIds.forEach(directorId -> jdbc.update(sql, filmId, directorId));
    }

    private MpaRating loadMpa(Long mpaRatingId) {
        return mpaRatingDbStorage.findById(mpaRatingId).orElse(null);
    }

    private Set<Genre> loadGenres(Long filmId) {
        String sql = "SELECT genre_id FROM film_genres WHERE film_id = ?";
        List<Long> genreIds = jdbc.queryForList(sql, Long.class, filmId);
        Set<Genre> genres = new HashSet<>();
        for (Long genreId : genreIds) {
            genreDbStorage.findById(genreId).ifPresent(genres::add);
        }
        return genres;
    }

    private Set<Long> loadDirectorIds(Long filmId) {
        String sql = "SELECT director_id FROM film_directors WHERE film_id = ?";
        List<Long> directorIds = jdbc.queryForList(sql, Long.class, filmId);
        return new HashSet<>(directorIds);
    }

    private void enrichFilm(Film film) {
        film.setMpa(loadMpa(film.getMpa().getId()));
        film.setGenres(loadGenres(film.getId()));
        film.setDirectorIds(loadDirectorIds(film.getId()));
    }

    private void enrichFilms(List<Film> films) {
        films.forEach(this::enrichFilm);
    }

    private final RowMapper<Film> filmMapper = (ResultSet rs, int rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getLong("mpa_rating_id"));
        film.setMpa(mpa);
        return film;
    };
}