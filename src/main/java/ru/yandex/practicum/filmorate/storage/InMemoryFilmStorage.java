package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component("inMemoryFilmStorage")
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Film create(Film film) {
        Long id = idGenerator.getAndIncrement();
        film.setId(id);
        films.put(id, film);
        log.debug("Фильм создан в памяти: {}", film);
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getId() == null || !films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        log.debug("Фильм обновлен в памяти: {}", film);
        return film;
    }

    @Override
    public Film updateDirectorsOnly(Film film) {
        if (film.getId() == null || !films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        Film existingFilm = films.get(film.getId());
        existingFilm.setDirectors(film.getDirectors());
        log.debug("Режиссеры фильма обновлены в памяти: id={}", existingFilm.getId());
        return existingFilm;
    }

    @Override
    public void delete(Long id) {
        if (id == null || !films.containsKey(id)) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        films.remove(id);
        log.debug("Фильм удален из памяти: id={}", id);
    }

    @Override
    public Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public List<Film> getFilmsByDirectorId(Long directorId, String sortBy) {
        List<Film> result = films.values().stream()
                .filter(film -> film.getDirectors() != null &&
                        film.getDirectors().stream().anyMatch(director -> directorId.equals(director.getId())))
                .collect(Collectors.toList());

        if ("year".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparing(Film::getReleaseDate));
        } else {
            result.sort(Comparator.comparing(Film::getReleaseDate).reversed());
        }

        return result;
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        String lowerQuery = query.toLowerCase();
        boolean searchByTitle = by.contains("title");
        boolean searchByDirector = by.contains("director");

        return films.values().stream()
                .filter(film -> {
                    boolean matchTitle = searchByTitle && film.getName().toLowerCase().contains(lowerQuery);

                    boolean matchDirector = false;
                    if (searchByDirector && film.getDirectors() != null) {
                        matchDirector = film.getDirectors().stream().anyMatch(director ->
                                (director.getName() != null && director.getName().toLowerCase().contains(lowerQuery)) ||
                                        (director.getSurname() != null && director.getSurname().toLowerCase().contains(lowerQuery))
                        );
                    }

                    return matchTitle || matchDirector;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return Collections.emptyList();
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        return Collections.emptyList();
    }
}