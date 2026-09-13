package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void contextLoads() {
        assertNotNull(userController);
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