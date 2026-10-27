package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.yandex.practicum.filmorate.storage.mappers.DirectorRowMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbc;
    private final DirectorRowMapper directorRowMapper;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (name, surname) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"director_id"});
            ps.setString(1, director.getName());
            ps.setString(2, director.getSurname());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            director.setId(key.longValue());
        }
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = "UPDATE directors SET name = ?, surname = ? WHERE director_id = ?";
        int rows = jdbc.update(sql, director.getName(), director.getSurname(), director.getId());
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + director.getId() + " не найден");
        }
        return director;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        int rows = jdbc.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден");
        }
    }

    @Override
    public Optional<Director> findById(Long id) {
        String sql = "SELECT director_id, name, surname FROM directors WHERE director_id = ?";
        List<Director> directors = jdbc.query(sql, directorRowMapper, id);
        return directors.isEmpty() ? Optional.empty() : Optional.of(directors.get(0));
    }

    @Override
    public List<Director> findAll() {
        String sql = "SELECT director_id, name, surname FROM directors ORDER BY director_id";
        return jdbc.query(sql, directorRowMapper);
    }

    public List<Film> findFilmsByDirector(Long directorId, String sortBy) {
        String sql;
        if ("year".equalsIgnoreCase(sortBy)) {
            sql = """
                    SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id
                    FROM films f
                    JOIN film_directors fd ON f.film_id = fd.film_id
                    WHERE fd.director_id = ?
                    ORDER BY f.release_date
                    """;
        } else {
            sql = """
                    SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id
                    FROM films f
                    JOIN film_directors fd ON f.film_id = fd.film_id
                    LEFT JOIN likes l ON f.film_id = l.film_id
                    WHERE fd.director_id = ?
                    GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id
                    ORDER BY COUNT(l.user_id) DESC
                    """;
        }

        List<Film> films = jdbc.query(sql, filmMapper, directorId);
        enrichFilms(films);
        return films;
    }

    private void enrichFilms(List<Film> films) {
        films.forEach(this::enrichFilm);
    }

    private void enrichFilm(Film film) {
        film.setMpa(loadMpa(film.getMpa().getId()));
        film.setGenres(loadGenres(film.getId()));
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
}