package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql(scripts = {"classpath:schema.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("userDbStorage")
    private UserStorage userStorage;

    @Autowired
    @Qualifier("filmDbStorage")
    private FilmStorage filmStorage;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private User user1;
    private User user2;
    private Film film;

    @BeforeEach
    void setUp() {
        ensureReferenceDataExists();

        jdbc.execute("DELETE FROM events");
        jdbc.update("DELETE FROM friendships WHERE user_id > 3 OR friend_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");
        jdbc.update("DELETE FROM users WHERE user_id > 3");

        if (jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class) > 0) {
            jdbc.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM films", Integer.class) > 0) {
            jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        }
        jdbc.update("ALTER TABLE events ALTER COLUMN event_id RESTART WITH 1");

        user1 = userStorage.create(createUser("feed_u1@example.com", "feed_u1"));
        user2 = userStorage.create(createUser("feed_u2@example.com", "feed_u2"));
        film = filmStorage.create(createFilm("Test Feed Film", LocalDate.of(2020, 1, 1)));
    }

    private void ensureReferenceDataExists() {
        Integer mpaCount = jdbc.queryForObject("SELECT COUNT(*) FROM mpa_ratings", Integer.class);
        if (mpaCount == null || mpaCount == 0) {
            jdbc.update("MERGE INTO mpa_ratings (mpa_rating_id, name, description) KEY (mpa_rating_id) VALUES " +
                    "(1, 'G', 'У фильма нет возрастных ограничений')," +
                    "(2, 'PG', 'Детям рекомендуется смотреть фильм с родителями')," +
                    "(3, 'PG-13', 'Детям до 13 лет просмотр не желателен')," +
                    "(4, 'R', 'Лицам до 17 лет просматривать фильм только в сопровождении одного из родителей')," +
                    "(5, 'NC-17', 'Лицам до 18 лет просмотр запрещён')");
        }

        Integer genreCount = jdbc.queryForObject("SELECT COUNT(*) FROM genres", Integer.class);
        if (genreCount == null || genreCount == 0) {
            jdbc.update("MERGE INTO genres (genre_id, name) KEY (genre_id) VALUES " +
                    "(1, 'Комедия')," +
                    "(2, 'Драма')," +
                    "(3, 'Мультфильм')," +
                    "(4, 'Триллер')," +
                    "(5, 'Документальный')," +
                    "(6, 'Боевик')");
        }
    }

    @Test
    void getFeed_shouldReturnEmptyListForNewUser() throws Exception {
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getFeed_shouldRecordFriendAddEvent() throws Exception {
        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[0].entityId").value(user2.getId()))
                .andExpect(jsonPath("$[0].userId").value(user1.getId()));
    }

    @Test
    void getFeed_shouldRecordFriendRemoveEvent() throws Exception {
        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[1].operation").value("REMOVE"));
    }

    @Test
    void getFeed_shouldRecordLikeAddEvent() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("LIKE"))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[0].entityId").value(film.getId()))
                .andExpect(jsonPath("$[0].userId").value(user1.getId()));
        mockMvc.perform(get("/users/" + user2.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void getFeed_shouldRecordLikeRemoveEvent() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[1].operation").value("REMOVE"));
    }

    @Test
    void getFeed_shouldSortEventsByTimestampAsc() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        Thread.sleep(100);

        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].eventType").value("LIKE"))
                .andExpect(jsonPath("$[1].eventType").value("FRIEND"));
    }

    @Test
    void getFeed_nonExistentUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/users/999/feed"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFeed_eachEventShouldHaveAllRequiredFields() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").exists())
                .andExpect(jsonPath("$[0].timestamp").exists())
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].eventType").exists())
                .andExpect(jsonPath("$[0].operation").exists())
                .andExpect(jsonPath("$[0].entityId").exists());
    }

    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }

    private Film createFilm(String name, LocalDate releaseDate) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(releaseDate);
        film.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        film.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        Genre genre = new Genre();
        genre.setId(1L);
        genres.add(genre);
        film.setGenres(genres);

        return film;
    }
}
