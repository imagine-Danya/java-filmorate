package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DirectorDbStorageTest {

    @Autowired
    private DirectorStorage directorStorage;

    private Director director;

    @BeforeEach
    void setUp() {
        director = new Director();
        director.setName("Кристофер");
        director.setSurname("Нолан");
    }

    @Test
    void testCreateDirector() {
        Director created = directorStorage.create(director);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getId()).isGreaterThan(0);
        assertThat(created.getName()).isEqualTo("Кристофер");
        assertThat(created.getSurname()).isEqualTo("Нолан");
    }

    @Test
    void testFindById() {
        Director created = directorStorage.create(director);

        Optional<Director> found = directorStorage.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
        assertThat(found.get().getName()).isEqualTo("Кристофер");
        assertThat(found.get().getSurname()).isEqualTo("Нолан");
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Director> found = directorStorage.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void testFindByIdNull() {
        Optional<Director> found = directorStorage.findById(null);

        assertThat(found).isEmpty();
    }

    @Test
    void testFindAll() {
        directorStorage.create(director);

        Director director2 = new Director();
        director2.setName("Дэвид");
        director2.setSurname("Финчер");
        directorStorage.create(director2);

        List<Director> directors = directorStorage.findAll();

        assertThat(directors).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void testUpdate() {
        Director created = directorStorage.create(director);
        created.setName("Кристофер Дж.");

        Director updated = directorStorage.update(created);

        assertThat(updated.getName()).isEqualTo("Кристофер Дж.");

        Optional<Director> found = directorStorage.findById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Кристофер Дж.");
    }

    @Test
    void testUpdateNotFound() {
        Director nonExistent = new Director();
        nonExistent.setId(999L);
        nonExistent.setName("Тест");
        nonExistent.setSurname("Тестов");

        assertThrows(NotFoundException.class, () -> directorStorage.update(nonExistent));
    }

    @Test
    void testDelete() {
        Director created = directorStorage.create(director);
        Long id = created.getId();

        directorStorage.delete(id);

        assertThat(directorStorage.findById(id)).isEmpty();
    }

    @Test
    void testDeleteNotFound() {
        assertThrows(NotFoundException.class, () -> directorStorage.delete(999L));
    }
}
