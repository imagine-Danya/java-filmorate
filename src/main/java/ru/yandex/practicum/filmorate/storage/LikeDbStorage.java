package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikeDbStorage {

    private final JdbcTemplate jdbc;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final GenreDbStorage genreDbStorage;
    private final DirectorStorage directorStorage;

    public void addLike(Long filmId, Long userId) {
        jdbc.update("MERGE INTO likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        jdbc.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    public List<Long> getUserLikedFilms(Long userId) {
        return jdbc.queryForList("SELECT film_id FROM likes WHERE user_id = ?", Long.class, userId);
    }

    public List<Long> getUsersWhoLikedFilm(Long filmId) {
        return jdbc.queryForList("SELECT user_id FROM likes WHERE film_id = ?", Long.class, filmId);
    }

    public List<Film> getPopularFilms(int count, Long genreId, Integer year) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, COUNT(DISTINCT l.user_id) as likes_count ")
                .append("FROM films f ")
                .append("LEFT JOIN likes l ON f.film_id = l.film_id ");

        if (genreId != null) {
            sql.append("INNER JOIN film_genres fg ON f.film_id = fg.film_id AND fg.genre_id = ? ");
        }

        sql.append("WHERE 1=1 ");

        if (year != null) {
            sql.append("AND EXTRACT(YEAR FROM f.release_date) = ? ");
        }

        sql.append("GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id ")
                .append("ORDER BY likes_count DESC ")
                .append("LIMIT ?");

        Object[] params;
        if (genreId != null && year != null) {
            params = new Object[]{genreId, year, count};
        } else if (genreId != null) {
            params = new Object[]{genreId, count};
        } else if (year != null) {
            params = new Object[]{year, count};
        } else {
            params = new Object[]{count};
        }

        List<Film> films = jdbc.query(sql.toString(), filmMapper, params);
        enrichFilms(films);
        return films;
    }

    private void enrichFilms(List<Film> films) {
        films.forEach(this::enrichFilm);
    }

    private void enrichFilm(Film film) {
        mpaRatingDbStorage.findById(film.getMpa().getId()).ifPresent(film::setMpa);

        List<Long> genreIds = jdbc.queryForList("SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id ASC", Long.class, film.getId());
        Set<Genre> genres = new LinkedHashSet<>();
        for (Long id : genreIds) {
            genreDbStorage.findById(id).ifPresent(genres::add);
        }
        film.setGenres(genres);

        List<Long> directorIds = jdbc.queryForList("SELECT director_id FROM film_directors WHERE film_id = ? ORDER BY director_id ASC", Long.class, film.getId());
        Set<Director> directors = new LinkedHashSet<>();
        for (Long id : directorIds) {
            directorStorage.findById(id).ifPresent(directors::add);
        }
        film.setDirectors(directors);
    }

    private final RowMapper<Film> filmMapper = (rs, rowNum) -> {
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