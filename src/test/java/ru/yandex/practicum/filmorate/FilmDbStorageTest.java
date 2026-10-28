package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbc;

    private Film film;

    @Autowired
    public FilmDbStorageTest(FilmDbStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    @BeforeEach
    void setUp() {
        ensureReferenceDataExists();

        jdbc.update("DELETE FROM film_genres WHERE film_id > 3");
        jdbc.update("DELETE FROM film_directors WHERE film_id > 3");
        jdbc.update("DELETE FROM likes WHERE film_id > 3");
        jdbc.update("DELETE FROM films WHERE film_id > 3");

        if (jdbc.queryForObject("SELECT COUNT(*) FROM films", Integer.class) > 0) {
            jdbc.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 4");
        }

        film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        film.setMpa(mpa);

        Set<Genre> genres = new HashSet<>();
        Genre genre1 = new Genre();
        genre1.setId(1L);
        Genre genre2 = new Genre();
        genre2.setId(2L);
        genres.add(genre1);
        genres.add(genre2);
        film.setGenres(genres);
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

        Integer directorCount = jdbc.queryForObject("SELECT COUNT(*) FROM directors", Integer.class);
        if (directorCount == null || directorCount == 0) {
            jdbc.update("MERGE INTO directors (director_id, name, surname) KEY (director_id) VALUES " +
                    "(1, 'Иван', 'Иванов')," +
                    "(2, 'Петр', 'Петров')");
        }
    }

    @Test
    void testCreateFilm() {
        Film created = filmStorage.create(film);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Film");
        assertThat(created.getMpa().getId()).isEqualTo(1L);

        Set<Long> createdGenreIds = created.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        assertThat(createdGenreIds).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void testFindFilmById() {
        Film created = filmStorage.create(film);

        Optional<Film> found = filmStorage.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
        assertThat(found.get().getName()).isEqualTo("Test Film");
    }

    @Test
    void testFindFilmByIdNotFound() {
        Optional<Film> found = filmStorage.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void testUpdateFilm() {
        Film created = filmStorage.create(film);
        created.setName("Updated Film");

        Set<Genre> newGenres = new HashSet<>();
        Genre genre3 = new Genre();
        genre3.setId(3L);
        newGenres.add(genre3);
        created.setGenres(newGenres);

        Film updated = filmStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Updated Film");

        Set<Long> updatedGenreIds = updated.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
        assertThat(updatedGenreIds).containsExactlyInAnyOrder(3L);
    }

    @Test
    void testUpdateFilmNotFound() {
        Film nonExistent = new Film();
        nonExistent.setId(999L);
        nonExistent.setName("Test");
        nonExistent.setDescription("Test");
        nonExistent.setReleaseDate(LocalDate.of(2020, 1, 1));
        nonExistent.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        nonExistent.setMpa(mpa);

        assertThrows(NotFoundException.class, () -> filmStorage.update(nonExistent));
    }

    @Test
    void testFindAllFilms() {
        filmStorage.create(film);

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2021, 1, 1));
        film2.setDuration(90);

        MpaRating mpa2 = new MpaRating();
        mpa2.setId(2L);
        film2.setMpa(mpa2);

        Set<Genre> genres2 = new HashSet<>();
        Genre genre3 = new Genre();
        genre3.setId(3L);
        genres2.add(genre3);
        film2.setGenres(genres2);

        filmStorage.create(film2);

        Collection<Film> films = filmStorage.findAll();
        assertThat(films).hasSizeGreaterThanOrEqualTo(2);
    }
}