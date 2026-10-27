package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {"classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ReviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("userDbStorage")
    private UserStorage userStorage;

    @Autowired
    @Qualifier("filmDbStorage")
    private FilmStorage filmStorage;

    private User user1;
    private User user2;
    private Film film;

    @BeforeEach
    void setUp() {
        user1 = userStorage.create(createUser("user1@example.com", "user1"));
        user2 = userStorage.create(createUser("user2@example.com", "user2"));
        film = filmStorage.create(createFilm("Test Film", LocalDate.of(2020, 1, 1)));
    }

    @Test
    void createReview_shouldReturnCreatedReview() throws Exception {
        Review review = createReview("Great film!", true, user1.getId(), film.getId());

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").exists())
                .andExpect(jsonPath("$.content").value("Great film!"))
                .andExpect(jsonPath("$.isPositive").value(true))
                .andExpect(jsonPath("$.useful").value(0));
    }

    @Test
    void getReviewById_shouldReturnReview() throws Exception {
        Long reviewId = createAndReturnId("Test", true, user1.getId(), film.getId());

        mockMvc.perform(get("/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(reviewId))
                .andExpect(jsonPath("$.content").value("Test"));
    }

    @Test
    void updateReview_shouldReturnUpdatedReview() throws Exception {
        Long reviewId = createAndReturnId("Original", true, user1.getId(), film.getId());

        Review updated = new Review();
        updated.setReviewId(reviewId);
        updated.setContent("Updated");
        updated.setIsPositive(false);
        updated.setUserId(user1.getId());
        updated.setFilmId(film.getId());

        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated"))
                .andExpect(jsonPath("$.isPositive").value(false));
    }

    @Test
    void deleteReview_shouldRemoveReview() throws Exception {
        Long reviewId = createAndReturnId("To delete", true, user1.getId(), film.getId());

        mockMvc.perform(delete("/reviews/" + reviewId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/" + reviewId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLike_shouldIncreaseUseful() throws Exception {
        Long reviewId = createAndReturnId("Liked", true, user1.getId(), film.getId());

        mockMvc.perform(put("/reviews/" + reviewId + "/like/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(1));
    }

    @Test
    void addDislike_shouldDecreaseUseful() throws Exception {
        Long reviewId = createAndReturnId("Disliked", true, user1.getId(), film.getId());

        mockMvc.perform(put("/reviews/" + reviewId + "/dislike/" + user2.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/reviews/" + reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(-1));
    }

    @Test
    void getReviewsByFilmId_shouldReturnSortedByUseful() throws Exception {
        Long id1 = createAndReturnId("Review 1", true, user1.getId(), film.getId());
        Long id2 = createAndReturnId("Review 2", true, user2.getId(), film.getId());

        mockMvc.perform(put("/reviews/" + id1 + "/like/" + user1.getId()));
        mockMvc.perform(put("/reviews/" + id1 + "/like/" + user2.getId()));
        mockMvc.perform(put("/reviews/" + id2 + "/like/" + user1.getId()));

        mockMvc.perform(get("/reviews?filmId=" + film.getId() + "&count=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(id1))
                .andExpect(jsonPath("$[1].reviewId").value(id2));
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

    private Review createReview(String content, Boolean isPositive, Long userId, Long filmId) {
        Review review = new Review();
        review.setContent(content);
        review.setIsPositive(isPositive);
        review.setUserId(userId);
        review.setFilmId(filmId);
        return review;
    }

    private Long createAndReturnId(String content, Boolean isPositive, Long userId, Long filmId) throws Exception {
        Review review = createReview(content, isPositive, userId, filmId);
        String response = mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("reviewId").asLong();
    }
}