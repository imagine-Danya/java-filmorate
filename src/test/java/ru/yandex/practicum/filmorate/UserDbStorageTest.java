package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private UserStorage userStorage;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void testCreateUser() {
        User created = userStorage.create(user);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getId()).isGreaterThan(0);
        assertThat(created.getEmail()).isEqualTo("test@example.com");
        assertThat(created.getLogin()).isEqualTo("testuser");
        assertThat(created.getName()).isEqualTo("Test User");
    }

    @Test
    void testFindUserById() {
        User created = userStorage.create(user);

        Optional<User> found = userStorage.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getLogin()).isEqualTo("testuser");
    }

    @Test
    void testFindUserByIdNotFound() {
        Optional<User> found = userStorage.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void testUpdateUser() {
        User created = userStorage.create(user);
        created.setName("Updated Name");

        User updated = userStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getId()).isEqualTo(created.getId());
    }

    @Test
    void testUpdateUserNotFound() {
        User nonExistent = new User();
        nonExistent.setId(999L);
        nonExistent.setEmail("test@example.com");
        nonExistent.setLogin("testuser");
        nonExistent.setName("Test");
        nonExistent.setBirthday(LocalDate.of(1990, 1, 1));

        assertThrows(NotFoundException.class, () -> userStorage.update(nonExistent));
    }

    @Test
    void testFindAllUsers() {
        userStorage.create(user);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        userStorage.create(user2);

        Collection<User> users = userStorage.findAll();

        assertThat(users).hasSizeGreaterThanOrEqualTo(2);
    }
}