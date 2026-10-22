package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film film);

    Optional<Film> findById(Long id);

    Collection<Film> findAll();

    List<Film> findFilmsByName(String search);

    List<Film> findCommonFilms(Long firstId, Long secondId);

    List<Film> findRecommendations (Long userId);
}