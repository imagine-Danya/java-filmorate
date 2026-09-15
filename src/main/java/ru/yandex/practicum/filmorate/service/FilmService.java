package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Film create(Film film) {
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        return filmStorage.update(film);
    }

    public Film findById(Long id) {
        return filmStorage.findById(id).orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public List<Film> findAll() {
        return List.copyOf(filmStorage.findAll());
    }

    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        film.getLikes().add(userId);
        // Не вызываем filmStorage.update(), так как работаем со ссылкой на объект в памяти
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        film.getLikes().remove(userId);
        // Не вызываем filmStorage.update(), так как работаем со ссылкой на объект в памяти
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}