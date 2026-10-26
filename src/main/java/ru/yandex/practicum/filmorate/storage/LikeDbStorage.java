package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class LikeDbStorage {

    private final JdbcTemplate jdbc;

    public void addLike(Long filmId, Long userId) {
        String sql = "MERGE INTO likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbc.update(sql, filmId, userId);
        log.debug("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbc.update(sql, filmId, userId);
        log.debug("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count, Long genreId, Integer year) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id ")
                .append("FROM films f ")
                .append("LEFT JOIN likes l ON f.film_id = l.film_id ");

        if (genreId != null) {
            sql.append("JOIN film_genres fg ON f.film_id = fg.film_id ");
        }

        sql.append("WHERE 1=1 ");

        if (genreId != null) {
            sql.append("AND fg.genre_id = ? ");
        }

        if (year != null) {
            sql.append("AND EXTRACT(YEAR FROM f.release_date) = ? ");
        }

        sql.append("GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id ")
                .append("ORDER BY COUNT(l.user_id) DESC ")
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

        return jdbc.query(sql.toString(), filmMapper, params);
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