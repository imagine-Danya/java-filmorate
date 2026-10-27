package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.Collection;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> create(@Valid @RequestBody Review review) {
        log.info("POST /reviews: создание отзыва");
        return ResponseEntity.ok(reviewService.create(review));
    }

    @PutMapping
    public ResponseEntity<Review> update(@Valid @RequestBody Review review) {
        log.info("PUT /reviews: обновление отзыва");
        return ResponseEntity.ok(reviewService.update(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /reviews/{}: удаление отзыва", id);
        reviewService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> findById(@PathVariable Long id) {
        log.info("GET /reviews/{}: получение отзыва по id", id);
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Collection<Review>> getReviews(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") int count) {
        log.info("GET /reviews: filmId={}, count={}", filmId, count);
        if (filmId != null) {
            return ResponseEntity.ok(reviewService.findByFilmId(filmId, count));
        }
        return ResponseEntity.ok(reviewService.findAll(count));
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Review> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/like/{}: лайк отзыву", id, userId);
        reviewService.addLike(id, userId);
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @PutMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Review> addDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("PUT /reviews/{}/dislike/{}: дизлайк отзыву", id, userId);
        reviewService.addDislike(id, userId);
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Review> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/like/{}: удаление лайка", id, userId);
        reviewService.removeLike(id, userId);
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Review> removeDislike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("DELETE /reviews/{}/dislike/{}: удаление дизлайка", id, userId);
        reviewService.removeDislike(id, userId);
        return ResponseEntity.ok(reviewService.findById(id));
    }
}