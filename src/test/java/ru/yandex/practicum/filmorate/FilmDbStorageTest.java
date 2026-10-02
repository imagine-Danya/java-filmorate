package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
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
        film.setMpaRatingId(1L);
        film.setGenres(Set.of(1L, 2L));  // ИСПРАВЛЕНО: было setGenreIds
    }

    @Test
    void testCreateFilm() {
        Film created = filmStorage.create(film);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Film");
        assertThat(created.getMpaRatingId()).isEqualTo(1L);
        assertThat(created.getGenres()).containsExactlyInAnyOrder(1L, 2L);  // ИСПРАВЛЕНО: было getGenreIds
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
        created.setGenres(Set.of(3L));  // ИСПРАВЛЕНО: было setGenreIds

        Film updated = filmStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Updated Film");
        assertThat(updated.getGenres()).containsExactlyInAnyOrder(3L);  // ИСПРАВЛЕНО: было getGenreIds
    }

    @Test
    void testUpdateFilmNotFound() {
        film.setId(999L);

        assertThrows(Exception.class, () -> filmStorage.update(film));
    }

    @Test
    void testFindAllFilms() {
        filmStorage.create(film);

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2021, 1, 1));
        film2.setDuration(90);
        film2.setMpaRatingId(2L);
        film2.setGenres(Set.of(3L));  // ИСПРАВЛЕНО: было setGenreIds
        filmStorage.create(film2);

        Collection<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(2);
    }
}