package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;

    @Qualifier("userDbStorage")
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
        return filmStorage.findById(id).orElseThrow(() ->
                new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public List<Film> findAll() {
        return List.copyOf(filmStorage.findAll());
    }

    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";

        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, COUNT(l.user_id) AS likes_count " +
                "FROM films f LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id ORDER BY likes_count DESC LIMIT ?";

        return List.of();
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}