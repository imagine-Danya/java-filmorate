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
import ru.yandex.practicum.filmorate.model.Director;
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
    private final DirectorStorage directorStorage;

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

        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectors());
        }

        log.debug("Фильм создан в БД: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE film_id = ?";
        String deleteDirector = "DELETE FROM film_directors WHERE film_id = ?";
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

        jdbc.update(deleteDirector, film.getId());
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectors());
        }

        log.debug("Фильм обновлен в БД: {}", film);
        return film;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films WHERE film_id = ?";
        List<Film> films = jdbc.query(sql, filmMapper, id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        film.setMpa(loadMpa(film.getMpa().getId()));
        film.setGenres(loadGenres(film.getId()));
        film.setDirectors(loadDirectors(film.getId()));
        return Optional.of(film);
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films";
        List<Film> films = jdbc.query(sql, filmMapper);

        films.forEach(film -> {
            film.setMpa(loadMpa(film.getMpa().getId()));
            film.setGenres(loadGenres(film.getId()));
            film.setDirectors(loadDirectors(film.getId()));
        });

        return films;
    }

    @Override
    public List<Film> findFilmsByName(String search) {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id " +
                "FROM films WHERE LOWER(name) LIKE LOWER(?)";
        String searchPattern = "%" + search + "%";

        List<Film> films = jdbc.query(sql, filmMapper, searchPattern);

        films.forEach(film -> {
            film.setMpa(loadMpa(film.getMpa().getId()));
            film.setGenres(loadGenres(film.getId()));
            film.setDirectors(loadDirectors(film.getId()));
        });

        return films;
    }

    @Override
    public List<Film> findCommonFilms(Long firstId, Long secondId) {
        String forLikes = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films f " +
                "JOIN ( " +
                "    SELECT film_id FROM likes " +
                "    WHERE user_id IN (?, ?) " +
                "    GROUP BY film_id HAVING COUNT(user_id) = 2 " +
                ") AS common ON f.film_id = common.film_id";

        List<Film> films = jdbc.query(forLikes, filmMapper, firstId, secondId);

        films.forEach(film -> {
            film.setMpa(loadMpa(film.getMpa().getId()));
            film.setGenres(loadGenres(film.getId()));
            film.setDirectors(loadDirectors(film.getId()));
        });
        return films;
    }

    @Override
    public List<Film> findRecommendations(Long userId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films AS f " +
                "JOIN likes AS d ON f.film_id = d.film_id " +
                "WHERE f.film_id IN ( " +
                "    SELECT c.film_id FROM likes AS c WHERE c.user_id IN ( " +
                "        SELECT b.user_id FROM likes AS b WHERE b.film_id IN ( " +
                "            SELECT a.film_id FROM likes AS a WHERE a.user_id = ? " +
                "        ) " +
                "    ) " +
                ") " +
                "AND d.user_id != ? " +
                "AND f.film_id NOT IN ( " +
                "    SELECT film_id FROM likes WHERE user_id = ? " +
                ") " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "ORDER BY COUNT(d.user_id) DESC " +
                "LIMIT 10";

        List<Film> films = jdbc.query(sql, filmMapper, userId, userId, userId);
        films.forEach(film -> {
            film.setMpa(loadMpa(film.getMpa().getId()));
            film.setGenres(loadGenres(film.getId()));
            film.setDirectors(loadDirectors(film.getId()));
        });
        return films;
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        genres.forEach(genre -> jdbc.update(sql, filmId, genre.getId()));
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

    private void saveDirectors(Long filmId, Set<Director> directors) {
        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        directors.forEach(director ->
                jdbc.update(sql, filmId, director.getId()));
    }

    private Set<Director> loadDirectors(Long filmId) {
        String sql = "SELECT director_id FROM film_directors WHERE film_id = ?";
        List<Long> directorIds = jdbc.queryForList(
                sql, Long.class, filmId);
        Set<Director> directors = new HashSet<>();
        for (Long directorId : directorIds) {
            directorStorage.findById(directorId).ifPresent(directors::add);
        }
        return directors;
    }
}