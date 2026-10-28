package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewDbStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventDbStorage eventStorage;

    public Review create(Review review) {
        if (review.getUserId() == null) {
            throw new ValidationException("ID пользователя не может быть null");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("ID фильма не может быть null");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Поле isPositive не может быть null");
        }
        if (userStorage.findById(review.getUserId()).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + review.getUserId() + " не найден");
        }
        if (filmStorage.findById(review.getFilmId()).isEmpty()) {
            throw new NotFoundException("Фильм с id " + review.getFilmId() + " не найден");
        }

        Review created = reviewStorage.create(review);

        Event event = new Event();
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(review.getUserId());
        event.setEventType("REVIEW");
        event.setOperation("ADD");
        event.setEntityId(created.getReviewId());
        eventStorage.create(event);

        return created;
    }

    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        Review existing = reviewStorage.findById(review.getReviewId())
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + review.getReviewId() + " не найден"));

        review.setUserId(existing.getUserId());
        review.setFilmId(existing.getFilmId());

        Review updated = reviewStorage.update(review);

        Event event = new Event();
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(existing.getUserId());
        event.setEventType("REVIEW");
        event.setOperation("UPDATE");
        event.setEntityId(review.getReviewId());
        eventStorage.create(event);

        return updated;
    }

    public void delete(Long id) {
        Review review = reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден"));

        reviewStorage.delete(id);

        Event event = new Event();
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(review.getUserId());
        event.setEventType("REVIEW");
        event.setOperation("REMOVE");
        event.setEntityId(id);
        eventStorage.create(event);
    }

    public Review findById(Long id) {
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id " + id + " не найден"));
    }

    public Collection<Review> findByFilmId(Long filmId, int count) {
        if (count < 0) {
            throw new ValidationException("Параметр count не может быть отрицательным");
        }
        return reviewStorage.findByFilmId(filmId, count);
    }

    public Collection<Review> findAll(int count) {
        if (count < 0) {
            throw new ValidationException("Параметр count не может быть отрицательным");
        }
        return reviewStorage.findAll(count);
    }

    public void addLike(Long reviewId, Long userId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        reviewStorage.addLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeLike(Long reviewId, Long userId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        reviewStorage.removeLike(reviewId, userId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id " + reviewId + " не найден");
        }
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        reviewStorage.removeDislike(reviewId, userId);
    }
}