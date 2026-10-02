package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;

    private Film film;

    @BeforeEach
    void setUp() {
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

        assertThat(films).hasSize(2);
    }
}