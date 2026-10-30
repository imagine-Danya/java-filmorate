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
                "m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "WHERE f.film_id = ?";
        List<Film> films = jdbc.query(sql, filmWithMpaMapper, id);
        if (films.isEmpty()) return Optional.empty();

        Film film = films.get(0);
        enrichFilm(film);
        return Optional.of(film);
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "ORDER BY f.film_id";
        List<Film> films = jdbc.query(sql, filmWithMpaMapper);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getFilmsByDirectorId(Long directorId, String sortBy) {
        if (directorStorage.findById(directorId).isEmpty()) {
            throw new NotFoundException("Режиссёр с id " + directorId + " не найден");
        }

        String orderBy = "year".equalsIgnoreCase(sortBy) ? "f.release_date ASC" : "COUNT(l.user_id) DESC";

        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name, m.description " +
                "ORDER BY " + orderBy;

        List<Film> films = jdbc.query(sql, filmWithMpaMapper, directorId);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        String searchPattern = "%" + query.toLowerCase() + "%";
        boolean searchByTitle = by.contains("title");
        boolean searchByDirector = by.contains("director");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, ")
                .append("m.name as mpa_name, m.description as mpa_description ")
                .append("FROM films f ")
                .append("LEFT JOIN film_directors fd ON f.film_id = fd.film_id ")
                .append("LEFT JOIN directors d ON fd.director_id = d.director_id ")
                .append("LEFT JOIN likes l ON f.film_id = l.film_id ")
                .append("LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id ")
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

        sql.append(" GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, ");
        sql.append("m.name, m.description ");
        sql.append("ORDER BY COUNT(l.user_id) DESC");

        List<Film> films = jdbc.query(sql.toString(), filmWithMpaMapper, params.toArray());
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name as mpa_name, m.description as mpa_description, " +
                "d.director_id, d.name as director_name, d.surname as director_surname " +
                "FROM films f " +
                "JOIN ( SELECT film_id FROM likes WHERE user_id = ? INTERSECT SELECT film_id FROM likes WHERE user_id = ? ) common ON f.film_id = common.film_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "LEFT JOIN film_directors fd ON f.film_id = fd.film_id " +
                "LEFT JOIN directors d ON fd.director_id = d.director_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, " +
                "m.name, m.description, d.director_id, d.name, d.surname " +
                "ORDER BY COUNT(l.user_id) DESC";

        Map<Long, Film> filmMap = new LinkedHashMap<>();
        Map<Long, Set<Director>> directorsMap = new HashMap<>();

        jdbc.query(sql, (rs, rowNum) -> {
            Long filmId = rs.getLong("film_id");
            if (!filmMap.containsKey(filmId)) {
                Film film = new Film();
                film.setId(filmId);
                film.setName(rs.getString("name"));
                film.setDescription(rs.getString("description"));
                film.setReleaseDate(rs.getDate("release_date").toLocalDate());
                film.setDuration(rs.getInt("duration"));

                MpaRating mpa = new MpaRating();
                mpa.setId(rs.getLong("mpa_rating_id"));
                mpa.setName(rs.getString("mpa_name"));
                mpa.setDescription(rs.getString("mpa_description"));
                film.setMpa(mpa);
                film.setDirectors(new LinkedHashSet<>());
                film.setGenres(new LinkedHashSet<>());

                filmMap.put(filmId, film);
            }

            Long directorId = rs.getObject("director_id") != null ? rs.getLong("director_id") : null;
            if (directorId != null) {
                Director director = new Director();
                director.setId(directorId);
                director.setName(rs.getString("director_name"));
                director.setSurname(rs.getString("director_surname"));
                directorsMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(director);
            }
            return null;
        }, userId, friendId);

        List<Film> films = new ArrayList<>(filmMap.values());

        if (!films.isEmpty()) {
            Set<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());
            String genresSql = "SELECT fg.film_id, g.genre_id, g.name " +
                    "FROM film_genres fg JOIN genres g ON fg.genre_id = g.genre_id " +
                    "WHERE fg.film_id IN (" + filmIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";

            Map<Long, Set<Genre>> genresMap = new HashMap<>();
            jdbc.query(genresSql, (rs, rowNum) -> {
                Long fId = rs.getLong("film_id");
                Genre genre = new Genre();
                genre.setId(rs.getLong("genre_id"));
                genre.setName(rs.getString("name"));
                genresMap.computeIfAbsent(fId, k -> new LinkedHashSet<>()).add(genre);
                return null;
            }, filmIds.toArray());

            for (Film film : films) {
                film.setDirectors(directorsMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
                film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
            }
        }

        return films;
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
                "m.name as mpa_name, m.description as mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_rating_id " +
                "WHERE f.film_id IN (" + placeholders + ")";

        List<Film> films = jdbc.query(sql, filmWithMpaMapper, ids.toArray());
        enrichFilms(films);

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

    private void enrichFilm(Film film) {
        if (film.getMpa() != null && film.getMpa().getName() == null) {
            mpaRatingDbStorage.findById(film.getMpa().getId()).ifPresent(film::setMpa);
        }
        film.setGenres(loadGenres(film.getId()));
        film.setDirectors(loadDirectors(film.getId()));
    }

    private void enrichFilms(List<Film> films) {
        if (films.isEmpty()) return;

        Set<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toSet());

        String genresSql = "SELECT fg.film_id, g.genre_id, g.name " +
                "FROM film_genres fg JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + filmIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") " +
                "ORDER BY fg.film_id, g.genre_id";

        Map<Long, Set<Genre>> genresMap = new HashMap<>();
        jdbc.query(genresSql, (rs, rowNum) -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getLong("genre_id"));
            genre.setName(rs.getString("name"));
            genresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            return null;
        }, filmIds.toArray());

        String directorsSql = "SELECT fd.film_id, d.director_id, d.name, d.surname " +
                "FROM film_directors fd JOIN directors d ON fd.director_id = d.director_id " +
                "WHERE fd.film_id IN (" + filmIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") " +
                "ORDER BY fd.film_id, d.director_id";

        Map<Long, Set<Director>> directorsMap = new HashMap<>();
        jdbc.query(directorsSql, (rs, rowNum) -> {
            Long filmId = rs.getLong("film_id");
            Director director = new Director();
            director.setId(rs.getLong("director_id"));
            director.setName(rs.getString("name"));
            director.setSurname(rs.getString("surname"));
            directorsMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(director);
            return null;
        }, filmIds.toArray());

        for (Film film : films) {
            if (film.getMpa() != null && film.getMpa().getName() == null) {
                mpaRatingDbStorage.findById(film.getMpa().getId()).ifPresent(film::setMpa);
            }
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
            film.setDirectors(directorsMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }

    private Set<Genre> loadGenres(Long filmId) {
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

    private final RowMapper<Film> filmWithMpaMapper = (ResultSet rs, int rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getLong("mpa_rating_id"));
        mpa.setName(rs.getString("mpa_name"));
        mpa.setDescription(rs.getString("mpa_description"));
        film.setMpa(mpa);
        return film;
    };
}