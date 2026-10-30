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
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "g.genre_id, g.name as genre_name, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_genres fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.genre_id " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "WHERE f.film_id = ?";

        List<Film> films = jdbc.query(sql, new FilmRowMapper(), id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(films.get(0));
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "g.genre_id, g.name as genre_name, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_genres fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.genre_id " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "ORDER BY f.film_id";

        return jdbc.query(sql, new FilmRowMapper());
    }

    @Override
    public List<Film> getFilmsByDirectorId(Long directorId, String sortBy) {
        if (directorStorage.findById(directorId).isEmpty()) {
            throw new NotFoundException("Режиссёр с id " + directorId + " не найден");
        }

        String orderBy = "year".equalsIgnoreCase(sortBy) ? "f.release_date ASC" : "COUNT(l.user_id) DESC";

        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "g.genre_id, g.name as genre_name, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_genres fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.genre_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name, m.description, g.genre_id, g.name, d.director_id, d.name, d.surname " +
                "ORDER BY " + orderBy;

        return jdbc.query(sql, new FilmRowMapper(), directorId);
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        boolean searchByTitle = by.contains("title");
        boolean searchByDirector = by.contains("director");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, ")
                .append("m.name as mpa_name, m.description as mpa_description, ")
                .append("g.genre_id, g.name as genre_name, ")
                .append("d.director_id, d.name as director_name, d.surname as director_surname ")
                .append("FROM films f ")
                .append("LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id ")
                .append("LEFT JOIN film_genres fg ON f.film_id = fg.film_id ")
                .append("LEFT JOIN genres g ON fg.genre_id = g.genre_id ");

        List<Object> params = new ArrayList<>();

        if (searchByDirector) {
            sql.append("LEFT JOIN film_directors fd ON f.film_id = fd.film_id ")
                    .append("LEFT JOIN directors d ON fd.director_id = d.director_id ");
        } else {
            sql.append("LEFT JOIN film_directors fd ON f.film_id = fd.film_id ")
                    .append("LEFT JOIN directors d ON fd.director_id = d.director_id ");
        }

        sql.append("LEFT JOIN likes l ON f.film_id = l.film_id ")
                .append("WHERE ");

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

        sql.append(" GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, ");
        sql.append("m.name, m.description, g.genre_id, g.name, d.director_id, d.name, d.surname ");
        sql.append("ORDER BY COUNT(l.user_id) DESC");

        return jdbc.query(sql.toString(), new FilmRowMapper(), params.toArray());
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "g.genre_id, g.name as genre_name, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "JOIN ( SELECT film_id FROM likes WHERE user_id = ? INTERSECT SELECT film_id FROM likes WHERE user_id = ? ) common ON f.film_id = common.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_genres fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.genre_id " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name, m.description, g.genre_id, g.name, d.director_id, d.name, d.surname " +
                "ORDER BY COUNT(l.user_id) DESC";

        return jdbc.query(sql, new FilmRowMapper(), userId, friendId);
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        return Collections.emptyList();
    }

    @Override
    public List<Film> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "g.genre_id, g.name as genre_name, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_genres fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres g ON fg.genre_id = g.genre_id " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "WHERE f.film_id IN (" + placeholders + ")";

        List<Film> films = jdbc.query(sql, new FilmRowMapper(), ids.toArray());

        Map<Long, Film> filmMap = films.stream().collect(Collectors.toMap(Film::getId, f -> f));
        return ids.stream()
                .map(filmMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    private static class FilmRowMapper implements RowMapper<Film> {
        private final Map<Long, Film> filmCache = new LinkedHashMap<>();

        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long filmId = rs.getLong("film_id");

            if (filmCache.containsKey(filmId)) {
                Film existingFilm = filmCache.get(filmId);
                Long genreId = rs.getObject("genre_id") != null ? rs.getLong("genre_id") : null;
                if (genreId != null) {
                    Genre genre = new Genre();
                    genre.setId(genreId);
                    genre.setName(rs.getString("genre_name"));
                    existingFilm.getGenres().add(genre);
                }
                Long directorId = rs.getObject("director_id") != null ? rs.getLong("director_id") : null;
                if (directorId != null) {
                    Director director = new Director();
                    director.setId(directorId);
                    director.setName(rs.getString("director_name"));
                    director.setSurname(rs.getString("director_surname"));
                    existingFilm.getDirectors().add(director);
                }
                return existingFilm;
            }

            Film film = new Film();
            film.setId(filmId);
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));

            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("mpa_rating_id"));
            String mpaName = rs.getString("mpa_name");
            if (mpaName != null) {
                mpa.setName(mpaName);
                mpa.setDescription(rs.getString("mpa_description"));
            }
            film.setMpa(mpa);

            Set<Genre> genres = new LinkedHashSet<>();
            Long genreId = rs.getObject("genre_id") != null ? rs.getLong("genre_id") : null;
            if (genreId != null) {
                Genre genre = new Genre();
                genre.setId(genreId);
                genre.setName(rs.getString("genre_name"));
                genres.add(genre);
            }
            film.setGenres(genres);

            Set<Director> directors = new LinkedHashSet<>();
            Long directorId = rs.getObject("director_id") != null ? rs.getLong("director_id") : null;
            if (directorId != null) {
                Director director = new Director();
                director.setId(directorId);
                director.setName(rs.getString("director_name"));
                director.setSurname(rs.getString("director_surname"));
                directors.add(director);
            }
            film.setDirectors(directors);

            filmCache.put(filmId, film);
            return film;
        }

        public List<Film> getFilms() {
            return new ArrayList<>(filmCache.values());
        }
    }
}