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

        return findById(film.getId()).orElse(film);
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
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectors());
        }

        return findById(film.getId()).orElse(film);
    }

    @Override
    public Film updateDirectorsOnly(Film film) {
        jdbc.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            saveDirectors(film.getId(), film.getDirectors());
        }
        return findById(film.getId()).orElse(film);
    }

    @Override
    public void delete(Long id) {
        Optional<Film> film = findById(id);
        if (film.isEmpty()) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }

        jdbc.update("DELETE FROM film_genres WHERE film_id = ?", id);
        jdbc.update("DELETE FROM film_directors WHERE film_id = ?", id);
        jdbc.update("DELETE FROM likes WHERE film_id = ?", id);
        jdbc.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films WHERE film_id = ?";
        List<Film> films = jdbc.query(sql, filmMapper, id);
        if (films.isEmpty()) return Optional.empty();

        Film film = films.get(0);
        enrichFilm(film);
        return Optional.of(film);
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films ORDER BY film_id";
        List<Film> films = jdbc.query(sql, filmMapper);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getFilmsByDirectorId(Long directorId, String sortBy) {
        // Сначала проверяем, существует ли режиссер
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

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id ")
                .append("FROM films f ")
                .append("LEFT JOIN film_directors fd ON f.film_id = fd.film_id ")
                .append("LEFT JOIN directors d ON fd.director_id = d.director_id ")
                .append("LEFT JOIN likes l ON f.film_id = l.film_id ")
                .append("WHERE ");

        List<Object> params = new ArrayList<>();

        if (searchByTitle && searchByDirector) {
            sql.append("(LOWER(f.name) LIKE ? OR LOWER(d.name) LIKE ? OR LOWER(d.surname) LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        } else if (searchByTitle) {
            sql.append("LOWER(f.name) LIKE ?");
            params.add(searchPattern);
        } else if (searchByDirector) {
            sql.append("(LOWER(d.name) LIKE ? OR LOWER(d.surname) LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
        }

        sql.append(" GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id ");
        sql.append("ORDER BY COUNT(l.user_id) DESC");

        List<Film> films = jdbc.query(sql.toString(), filmMapper, params.toArray());
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "FROM films f " +
                "JOIN ( SELECT film_id FROM likes WHERE user_id = ? INTERSECT SELECT film_id FROM likes WHERE user_id = ? ) common ON f.film_id = common.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id " +
                "ORDER BY COUNT(l.user_id) DESC";

        List<Film> films = jdbc.query(sql, filmMapper, userId, friendId);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        return Collections.emptyList();
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        for (Genre genre : genres) {
            jdbc.update("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)", filmId, genre.getId());
        }
    }

    private void saveDirectors(Long filmId, Set<Director> directors) {
        for (Director director : directors) {
            jdbc.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", filmId, director.getId());
        }
    }

    private Set<Genre> loadGenres(Long filmId) {
        // ORDER BY ASC — стабильный порядок [1, 3] вместо [3, 1]
        List<Long> genreIds = jdbc.queryForList(
                "SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id ASC",
                Long.class, filmId);
        Set<Genre> genres = new LinkedHashSet<>();
        for (Long id : genreIds) {
            genreDbStorage.findById(id).ifPresent(genres::add);
        }
        return genres;
    }

    private Set<Director> loadDirectors(Long filmId) {
        List<Long> directorIds = jdbc.queryForList(
                "SELECT director_id FROM film_directors WHERE film_id = ? ORDER BY director_id ASC",
                Long.class, filmId);
        Set<Director> directors = new LinkedHashSet<>();
        for (Long id : directorIds) {
            directorStorage.findById(id).ifPresent(directors::add);
        }
        return directors;
    }

    private void enrichFilm(Film film) {
        mpaRatingDbStorage.findById(film.getMpa().getId()).ifPresent(film::setMpa);
        film.setGenres(loadGenres(film.getId()));
        film.setDirectors(loadDirectors(film.getId()));
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