package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("filmDbStorage")
    private FilmStorage filmStorage;

    @Autowired
    @Qualifier("userDbStorage")
    private UserStorage userStorage;

    @Autowired
    private JdbcTemplate jdbc;

    private User user;
    private Film film;

    @BeforeEach
    void setUp() {

        jdbc.update("DELETE FROM film_genres WHERE film_id > 3");
        jdbc.update("DELETE FROM film_directors WHERE film_id > 3");
        jdbc.update("DELETE FROM likes WHERE film_id > 3 OR user_id > 3");
        jdbc.update("DELETE FROM friendships WHERE user_id > 3 OR friend_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");
        jdbc.update("DELETE FROM users WHERE user_id > 3");
        jdbc.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4");
        jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user = userStorage.create(user);
        film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);
        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        film.setMpa(mpa);
        Set<Genre> genres = new HashSet<>();
        Genre genre = new Genre();
        genre.setId(1L);
        genres.add(genre);
        film.setGenres(genres);

        film = filmStorage.create(film);
    }

    @Test
    void deleteUser_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void deleteUser_shouldRemoveUserFromDatabase() throws Exception {
        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user.getId()))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    void deleteUser_nonExistentUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void deleteFilm_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/films/" + film.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void deleteFilm_shouldRemoveFilmFromDatabase() throws Exception {
        mockMvc.perform(delete("/films/" + film.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + film.getId()))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    void deleteFilm_nonExistentFilm_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/films/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void deleteFilm_shouldAlsoRemoveGenres() throws Exception {
        Film filmWithGenres = new Film();
        filmWithGenres.setName("Film with genres");
        filmWithGenres.setDescription("Description");
        filmWithGenres.setReleaseDate(LocalDate.of(2021, 1, 1));
        filmWithGenres.setDuration(90);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        filmWithGenres.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        Genre genre1 = new Genre();
        genre1.setId(1L);
        Genre genre2 = new Genre();
        genre2.setId(2L);
        genres.add(genre1);
        genres.add(genre2);
        filmWithGenres.setGenres(genres);

        filmWithGenres = filmStorage.create(filmWithGenres);

        mockMvc.perform(delete("/films/" + filmWithGenres.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + filmWithGenres.getId()))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    void deleteUser_shouldNotAffectOtherUsers() throws Exception {
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        user2 = userStorage.create(user2);

        mockMvc.perform(delete("/users/" + user.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + user2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user2"))
                .andDo(print());
    }

    @Test
    void deleteFilm_shouldNotAffectOtherFilms() throws Exception {
        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2021, 1, 1));
        film2.setDuration(90);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        film2.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        Genre genre = new Genre();
        genre.setId(1L);
        genres.add(genre);
        film2.setGenres(genres);

        film2 = filmStorage.create(film2);

        mockMvc.perform(delete("/films/" + film.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/" + film2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Film 2"))
                .andDo(print());
    }
}