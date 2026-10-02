package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaRatingDbStorage;

import java.util.Collection;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
@Slf4j
public class MpaController {
    private final MpaRatingDbStorage mpaRatingDbStorage;

    @GetMapping
    public Collection<MpaRating> getAllMpaRatings() {
        log.info("GET /mpa: получение всех рейтингов MPA");
        return mpaRatingDbStorage.findAll();
    }

    @GetMapping("/{id}")
    public MpaRating getMpaRatingById(@PathVariable Long id) {
        log.info("GET /mpa/{}: получение рейтинга MPA по id", id);
        return mpaRatingDbStorage.findById(id)
                .orElseThrow(() -> new ru.yandex.practicum.filmorate.exception.NotFoundException("Рейтинг MPA с id " + id + " не найден"));
    }
}