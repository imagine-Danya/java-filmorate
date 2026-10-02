package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaRatingDbStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Component("filmDbStorage")
@Slf4j
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbc;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;

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

        return Optional.of(film);
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_rating_id FROM films";
        List<Film> films = jdbc.query(sql, filmMapper);

        films.forEach(film -> {
            film.setMpa(loadMpa(film.getMpa().getId()));
            film.setGenres(loadGenres(film.getId()));
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
}