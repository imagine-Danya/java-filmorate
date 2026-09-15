package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FilmStorage filmStorage;

    @Autowired
    private UserStorage userStorage;

    private Film film;
    private User user;

    @BeforeEach
    void setUp() {
        film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createFilm_shouldReturnCreatedFilm() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Film"))
                .andDo(print());
    }

    @Test
    void createFilm_withInvalidName_shouldReturnBadRequest() throws Exception {
        film.setName("");

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void createFilm_withInvalidReleaseDate_shouldReturnBadRequest() throws Exception {
        film.setReleaseDate(LocalDate.of(1800, 1, 1));

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void updateFilm_shouldReturnUpdatedFilm() throws Exception {
        Film created = filmStorage.create(film);
        created.setName("Updated Film");

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Film"))
                .andDo(print());
    }

    @Test
    void findAllFilms_shouldReturnListOfFilms() throws Exception {
        filmStorage.create(film);

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Test Film')]").exists())
                .andDo(print());
    }

    @Test
    void findById_shouldReturnFilm() throws Exception {
        Film created = filmStorage.create(film);

        mockMvc.perform(get("/films/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Test Film"))
                .andDo(print());
    }

    @Test
    void findById_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/films/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void addLike_shouldReturnOk() throws Exception {
        User createdUser = userStorage.create(user);
        Film createdFilm = filmStorage.create(film);

        mockMvc.perform(put("/films/" + createdFilm.getId() + "/like/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void addLike_withNonExistentFilm_shouldReturnNotFound() throws Exception {
        User createdUser = userStorage.create(user);

        mockMvc.perform(put("/films/999/like/" + createdUser.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void addLike_withNonExistentUser_shouldReturnNotFound() throws Exception {
        Film createdFilm = filmStorage.create(film);

        mockMvc.perform(put("/films/" + createdFilm.getId() + "/like/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void removeLike_shouldReturnOk() throws Exception {
        User createdUser = userStorage.create(user);
        Film createdFilm = filmStorage.create(film);
        createdFilm.getLikes().add(createdUser.getId());

        mockMvc.perform(delete("/films/" + createdFilm.getId() + "/like/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void removeLike_withNonExistentFilm_shouldReturnNotFound() throws Exception {
        User createdUser = userStorage.create(user);

        mockMvc.perform(delete("/films/999/like/" + createdUser.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void removeLike_withNonExistentUser_shouldReturnNotFound() throws Exception {
        Film createdFilm = filmStorage.create(film);

        mockMvc.perform(delete("/films/" + createdFilm.getId() + "/like/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void getPopularFilms_shouldReturnListOfFilms() throws Exception {
        filmStorage.create(film);

        mockMvc.perform(get("/films/popular?count=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }

    @Test
    void getPopularFilms_defaultCount_shouldReturnFilms() throws Exception {
        filmStorage.create(film);

        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }
}