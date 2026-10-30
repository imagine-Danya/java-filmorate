package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewDbStorage {

    private final JdbcTemplate jdbc;

    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) " +
                "VALUES (?, ?, ?, ?, 0)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"review_id"});
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            review.setReviewId(key.longValue());
        }
        review.setUseful(0);

        log.debug("Отзыв создан: {}", review);
        return review;
    }

    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";
        int rows = jdbc.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());

        if (rows == 0) {
            throw new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден");
        }

        log.debug("Отзыв обновлен: {}", review);
        return review;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        int rows = jdbc.update(sql, id);
        if (rows == 0) {
            throw new NotFoundException("Отзыв с id " + id + " не найден");
        }
        log.debug("Отзыв удален: id={}", id);
    }

    public Optional<Review> findById(Long id) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        List<Review> reviews = jdbc.query(sql, reviewMapper, id);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    public Collection<Review> findByFilmId(Long filmId, int count) {
        String sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
        return jdbc.query(sql, reviewMapper, filmId, count);
    }

    public Collection<Review> findAll(int count) {
        String sql = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
        return jdbc.query(sql, reviewMapper, count);
    }

    public void addLike(Long reviewId, Long userId) {
        String mergeSql = "MERGE INTO review_likes (review_id, user_id, is_like) " +
                "KEY (review_id, user_id) " +
                "VALUES (?, ?, true)";

        String checkSql = "SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbc.queryForList(checkSql, Boolean.class, reviewId, userId);

        if (!existing.isEmpty() && !existing.get(0)) {
            jdbc.update(mergeSql, reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful + 2 WHERE review_id = ?", reviewId);
        } else if (existing.isEmpty()) {
            jdbc.update(mergeSql, reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful + 1 WHERE review_id = ?", reviewId);
        }

        log.debug("Пользователь {} поставил лайк отзыву {}", userId, reviewId);
    }

    public void addDislike(Long reviewId, Long userId) {
        String mergeSql = "MERGE INTO review_likes (review_id, user_id, is_like) " +
                "KEY (review_id, user_id) " +
                "VALUES (?, ?, false)";

        String checkSql = "SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbc.queryForList(checkSql, Boolean.class, reviewId, userId);

        if (!existing.isEmpty() && existing.get(0)) {
            jdbc.update(mergeSql, reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful - 2 WHERE review_id = ?", reviewId);
        } else if (existing.isEmpty()) {
            jdbc.update(mergeSql, reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful - 1 WHERE review_id = ?", reviewId);
        }

        log.debug("Пользователь {} поставил дизлайк отзыву {}", userId, reviewId);
    }

    public void removeLike(Long reviewId, Long userId) {
        String checkSql = "SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbc.queryForList(checkSql, Boolean.class, reviewId, userId);

        if (!existing.isEmpty() && existing.get(0)) {
            jdbc.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful - 1 WHERE review_id = ?", reviewId);
        }
        log.debug("Пользователь {} удалил лайк у отзыва {}", userId, reviewId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        String checkSql = "SELECT is_like FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbc.queryForList(checkSql, Boolean.class, reviewId, userId);

        if (!existing.isEmpty() && !existing.get(0)) {
            jdbc.update("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?", reviewId, userId);
            jdbc.update("UPDATE reviews SET useful = useful + 1 WHERE review_id = ?", reviewId);
        }
        log.debug("Пользователь {} удалил дизлайк у отзыва {}", userId, reviewId);
    }

    private final RowMapper<Review> reviewMapper = (rs, rowNum) -> {
        Review review = new Review();
        review.setReviewId(rs.getLong("review_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getLong("user_id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUseful(rs.getInt("useful"));
        return review;
    };
}