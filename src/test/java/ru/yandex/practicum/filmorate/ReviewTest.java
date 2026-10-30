package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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

    @Autowired
    private JdbcTemplate jdbc;

    private User user1;
    private User user2;
    private Film film;

    @BeforeEach
    void setUp() {
        ensureReferenceDataExists();

        jdbc.update("DELETE FROM review_likes");
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM film_genres WHERE film_id > 3");
        jdbc.update("DELETE FROM film_directors WHERE film_id > 3");
        jdbc.update("DELETE FROM likes WHERE film_id > 3 OR user_id > 3");
        jdbc.update("DELETE FROM friendships WHERE user_id > 3 OR friend_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");
        jdbc.update("DELETE FROM users WHERE user_id > 3");

        if (jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class) > 0) {
            jdbc.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 4");
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM films", Integer.class) > 0) {
            jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        }
        jdbc.update("ALTER TABLE reviews ALTER COLUMN review_id RESTART WITH 1");

        user1 = userStorage.create(createUser("rev_u1@example.com", "rev_u1"));
        user2 = userStorage.create(createUser("rev_u2@example.com", "rev_u2"));
        film = filmStorage.create(createFilm("Test Review Film", LocalDate.of(2020, 1, 1)));
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