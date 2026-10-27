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
import ru.yandex.practicum.filmorate.storage.EventCollector;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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
    private EventCollector eventCollector;

    private User user1;
    private User user2;
    private Film film;

    @BeforeEach
    void setUp() {
        eventCollector.clear();

        user1 = createUser("user1@example.com", "user1");
        user2 = createUser("user2@example.com", "user2");
        user1 = userStorage.create(user1);
        user2 = userStorage.create(user2);

        film = createFilm("Test Film", LocalDate.of(2020, 1, 1));
        film = filmStorage.create(film);
    }

    @Test
    void getFeed_shouldReturnEmptyListForNewUser() throws Exception {
        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)))
                .andDo(print());
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
                .andExpect(jsonPath("$[0].userId").value(user1.getId()))
                .andDo(print());

        mockMvc.perform(get("/users/" + user2.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[0].entityId").value(user1.getId()))
                .andExpect(jsonPath("$[0].userId").value(user2.getId()))
                .andDo(print());
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
                .andExpect(jsonPath("$[0].operation").value("REMOVE"))
                .andExpect(jsonPath("$[1].operation").value("ADD"))
                .andDo(print());
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
                .andExpect(jsonPath("$[0].userId").value(user1.getId()))
                .andDo(print());

        mockMvc.perform(get("/users/" + user2.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)))
                .andDo(print());
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
                .andExpect(jsonPath("$[0].operation").value("REMOVE"))
                .andExpect(jsonPath("$[1].operation").value("ADD"))
                .andDo(print());
    }

    @Test
    void getFeed_shouldSortEventsByTimestampDesc() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        Thread.sleep(10);

        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[1].eventType").value("LIKE"))
                .andDo(print());
    }

    @Test
    void getFeed_nonExistentUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/users/999/feed"))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    void getFeed_shouldContainAllEventTypes() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/users/" + user1.getId() + "/friends/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[0].operation").value("REMOVE"))
                .andExpect(jsonPath("$[1].eventType").value("LIKE"))
                .andExpect(jsonPath("$[1].operation").value("REMOVE"))
                .andExpect(jsonPath("$[2].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[2].operation").value("ADD"))
                .andExpect(jsonPath("$[3].eventType").value("LIKE"))
                .andExpect(jsonPath("$[3].operation").value("ADD"))
                .andDo(print());
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
                .andExpect(jsonPath("$[0].entityId").exists())
                .andDo(print());
    }

    @Test
    void getFeed_shouldOnlyContainCurrentUserEvents() throws Exception {
        mockMvc.perform(put("/films/" + film.getId() + "/like/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/" + user2.getId() + "/friends/" + user1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user1.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andDo(print());

        mockMvc.perform(get("/users/" + user2.getId() + "/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(user2.getId()))
                .andDo(print());
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
        film.setDescription("Description of " + name);
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