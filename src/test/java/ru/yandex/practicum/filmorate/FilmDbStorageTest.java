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
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;
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
    private final DirectorStorage directorStorage;

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

    @Test
    void testCreateFilmWithDirectors() {
        Director nolan = createDirector("Кристофер", "Нолан");
        Director fincher = createDirector("Дэвид", "Финчер");

        Set<Director> directors = new HashSet<>();
        directors.add(nolan);
        directors.add(fincher);
        film.setDirectors(directors);

        Film created = filmStorage.create(film);

        assertThat(created.getDirectors()).hasSize(2);
        Set<Long> directorIds = created.getDirectors().stream()
                .map(Director::getId)
                .collect(Collectors.toSet());
        assertThat(directorIds).containsExactlyInAnyOrder(nolan.getId(), fincher.getId());
    }

    @Test
    void testCreateFilmWithoutDirectors() {
        film.setDirectors(new HashSet<>());

        Film created = filmStorage.create(film);

        assertThat(created.getDirectors()).isNotNull();
        assertThat(created.getDirectors()).isEmpty();
    }

    @Test
    void testFindFilmByIdReturnsDirectors() {
        Director nolan = createDirector("Кристофер", "Нолан");
        Set<Director> directors = new HashSet<>();
        directors.add(nolan);
        film.setDirectors(directors);

        Film created = filmStorage.create(film);

        Optional<Film> found = filmStorage.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDirectors()).hasSize(1);

        Director foundDirector = found.get().getDirectors().iterator().next();
        assertThat(foundDirector.getId()).isEqualTo(nolan.getId());
        assertThat(foundDirector.getName()).isEqualTo("Кристофер");
        assertThat(foundDirector.getSurname()).isEqualTo("Нолан");
    }

    @Test
    void testUpdateFilmReplacesDirectors() {
        Director nolan = createDirector("Кристофер", "Нолан");
        Director fincher = createDirector("Дэвид", "Финчер");

        Set<Director> initialDirectors = new HashSet<>();
        initialDirectors.add(nolan);
        film.setDirectors(initialDirectors);

        Film created = filmStorage.create(film);

        Set<Director> newDirectors = new HashSet<>();
        newDirectors.add(fincher);
        created.setDirectors(newDirectors);

        filmStorage.update(created);

        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isPresent();

        Set<Long> directorIds = found.get().getDirectors().stream()
                .map(Director::getId)
                .collect(Collectors.toSet());
        assertThat(directorIds).containsExactly(fincher.getId());
    }

    @Test
    void testUpdateFilmClearsDirectors() {
        Director nolan = createDirector("Кристофер", "Нолан");
        Set<Director> directors = new HashSet<>();
        directors.add(nolan);
        film.setDirectors(directors);

        Film created = filmStorage.create(film);

        created.setDirectors(new HashSet<>());
        filmStorage.update(created);

        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDirectors()).isEmpty();
    }

    private Director createDirector(String name, String surname) {
        Director director = new Director();
        director.setName(name);
        director.setSurname(surname);
        return directorStorage.create(director);
    }
}