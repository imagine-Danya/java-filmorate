package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PopularFilmsTest {

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

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {

        user1 = createUser("user1@example.com", "user1");
        user2 = createUser("user2@example.com", "user2");
        user3 = createUser("user3@example.com", "user3");

        userStorage.create(user1);
        userStorage.create(user2);
        userStorage.create(user3);
    }

    @Test
    void getPopularFilms_withoutFilters_shouldReturnAllFilmsSortedByLikes() throws Exception {

        Film comedy2020 = createFilm("Comedy 2020", LocalDate.of(2020, 1, 1), 1L, Set.of(1L)); // Комедия 2020
        Film drama2020 = createFilm("Drama 2020", LocalDate.of(2020, 1, 1), 1L, Set.of(2L));   // Драма 2020
        Film comedy2021 = createFilm("Comedy 2021", LocalDate.of(2021, 1, 1), 1L, Set.of(1L)); // Комедия 2021

        comedy2020 = filmStorage.create(comedy2020);
        drama2020 = filmStorage.create(drama2020);
        comedy2021 = filmStorage.create(comedy2021);

        addLike(comedy2020.getId(), user1.getId());
        addLike(comedy2020.getId(), user2.getId());
        addLike(comedy2020.getId(), user3.getId());

        addLike(drama2020.getId(), user1.getId());
        addLike(drama2020.getId(), user2.getId());

        addLike(comedy2021.getId(), user1.getId());

        mockMvc.perform(get("/films/popular?count=10")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3))).andExpect(jsonPath("$[0].name").value("Comedy 2020")).andExpect(jsonPath("$[1].name").value("Drama 2020")).andExpect(jsonPath("$[2].name").value("Comedy 2021")).andDo(print());
    }

    @Test
    void getPopularFilms_withGenreFilter_shouldReturnOnlyFilmsOfGenre() throws Exception {

        Film comedy = createFilm("Comedy Film", LocalDate.of(2020, 1, 1), 1L, Set.of(1L)); // Комедия
        Film drama = createFilm("Drama Film", LocalDate.of(2020, 1, 1), 1L, Set.of(2L));   // Драма

        comedy = filmStorage.create(comedy);
        drama = filmStorage.create(drama);

        addLike(comedy.getId(), user1.getId());
        addLike(comedy.getId(), user2.getId());
        addLike(drama.getId(), user1.getId());
        addLike(drama.getId(), user2.getId());
        addLike(drama.getId(), user3.getId());

        mockMvc.perform(get("/films/popular?count=10&genreId=1")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Comedy Film")).andDo(print());
        mockMvc.perform(get("/films/popular?count=10&genreId=2")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Drama Film")).andDo(print());
    }

    @Test
    void getPopularFilms_withYearFilter_shouldReturnOnlyFilmsOfYear() throws Exception {

        Film film2020 = createFilm("Film 2020", LocalDate.of(2020, 1, 1), 1L, Set.of(1L));
        Film film2021 = createFilm("Film 2021", LocalDate.of(2021, 1, 1), 1L, Set.of(1L));
        Film film2022 = createFilm("Film 2022", LocalDate.of(2022, 1, 1), 1L, Set.of(1L));

        film2020 = filmStorage.create(film2020);
        film2021 = filmStorage.create(film2021);
        film2022 = filmStorage.create(film2022);

        addLike(film2020.getId(), user1.getId());
        addLike(film2021.getId(), user1.getId());
        addLike(film2021.getId(), user2.getId());
        addLike(film2022.getId(), user1.getId());
        addLike(film2022.getId(), user2.getId());
        addLike(film2022.getId(), user3.getId());

        mockMvc.perform(get("/films/popular?count=10&year=2021")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Film 2021")).andDo(print());
        mockMvc.perform(get("/films/popular?count=10&year=2022")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Film 2022")).andDo(print());
    }

    @Test
    void getPopularFilms_withGenreAndYearFilter_shouldReturnFilteredFilms() throws Exception {

        Film comedy2020 = createFilm("Comedy 2020", LocalDate.of(2020, 1, 1), 1L, Set.of(1L));
        Film comedy2021 = createFilm("Comedy 2021", LocalDate.of(2021, 1, 1), 1L, Set.of(1L));
        Film drama2020 = createFilm("Drama 2020", LocalDate.of(2020, 1, 1), 1L, Set.of(2L));

        comedy2020 = filmStorage.create(comedy2020);
        comedy2021 = filmStorage.create(comedy2021);
        drama2020 = filmStorage.create(drama2020);

        addLike(comedy2020.getId(), user1.getId());
        addLike(comedy2020.getId(), user2.getId());
        addLike(comedy2021.getId(), user1.getId());
        addLike(drama2020.getId(), user1.getId());
        addLike(drama2020.getId(), user2.getId());
        addLike(drama2020.getId(), user3.getId());

        mockMvc.perform(get("/films/popular?count=10&genreId=1&year=2020")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Comedy 2020")).andDo(print());
        mockMvc.perform(get("/films/popular?count=10&genreId=2&year=2020")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Drama 2020")).andDo(print());
        mockMvc.perform(get("/films/popular?count=10&genreId=1&year=2021")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andExpect(jsonPath("$[0].name").value("Comedy 2021")).andDo(print());
    }

    @Test
    void getPopularFilms_withCountLimit_shouldReturnLimitedResults() throws Exception {
        User user4 = createUser("user4@example.com", "user4");
        User user5 = createUser("user5@example.com", "user5");
        userStorage.create(user4);
        userStorage.create(user5);
        List<User> allUsers = List.of(user1, user2, user3, user4, user5);
        for (int i = 1; i <= 5; i++) {
            Film film = createFilm("Film " + i, LocalDate.of(2020, 1, 1), 1L, Set.of(1L));
            film = filmStorage.create(film);
            for (int j = 0; j < i; j++) {
                addLike(film.getId(), allUsers.get(j).getId());
            }
        }

        mockMvc.perform(get("/films/popular?count=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Film 5"))
                .andExpect(jsonPath("$[1].name").value("Film 4"))
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

    private Film createFilm(String name, LocalDate releaseDate, Long mpaRatingId, Set<Long> genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description of " + name);
        film.setReleaseDate(releaseDate);
        film.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(mpaRatingId);
        film.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        for (Long genreId : genreIds) {
            Genre genre = new Genre();
            genre.setId(genreId);
            genres.add(genre);
        }
        film.setGenres(genres);

        return film;
    }

    private void addLike(Long filmId, Long userId) throws Exception {
        mockMvc.perform(put("/films/" + filmId + "/like/" + userId)).andExpect(status().isOk());
    }
}