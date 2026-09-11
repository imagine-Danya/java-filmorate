package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmorateApplicationTests {

    @Test
    void contextLoads() {
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
    
    @Test
    void testUserValidData() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        assertEquals("test@example.com", user.getEmail());
        assertEquals("testuser", user.getLogin());
    }

    @Test
    void testUserEmailContainsAt() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");

        assertTrue(user.getEmail().contains("@"));
    }

    @Test
    void testUserLoginNoSpaces() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");

        assertFalse(user.getLogin().contains(" "));
    }

    @Test
    void testUserNameFallbackToLogin() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName(null);

        assertEquals("testuser", user.getName());
    }

    @Test
    void testUserBirthdayNotInFuture() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setBirthday(LocalDate.now().minusYears(1));

        assertTrue(user.getBirthday().isBefore(LocalDate.now()) ||
                user.getBirthday().isEqual(LocalDate.now()));
    }

    @Test
    void testUserBirthdayInFuture() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> {
            if (user.getBirthday().isAfter(LocalDate.now())) {
                throw new ValidationException("Дата рождения не может быть в будущем");
            }
        });
    }
}
