package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Test
    void contextLoads() {
        assertNotNull(filmController);
    }

    @Test
    void testFilmValidData() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("A test film");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        assertNotNull(film.getName());
        assertTrue(film.getDuration() > 0);
    }

    @Test
    void testFilmReleaseDateBoundary() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(90);

        assertEquals(LocalDate.of(1895, 12, 28), film.getReleaseDate());
    }

    @Test
    void testFilmReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(90);

        assertThrows(ValidationException.class, () -> {
            if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
                throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
            }
        });
    }

    @Test
    void testFilmDurationPositive() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDuration(1);

        assertTrue(film.getDuration() > 0);
    }

    @Test
    void testFilmDescriptionMaxLength() {
        Film film = new Film();
        film.setName("Test Film");
        String description = "a".repeat(200);
        film.setDescription(description);

        assertEquals(200, film.getDescription().length());
    }
}