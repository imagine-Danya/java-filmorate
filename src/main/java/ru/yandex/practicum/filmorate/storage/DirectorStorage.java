package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;

public interface DirectorStorage {
    Optional<Director> findById(Long id);
    List<Director> findAll();
    Director create(Director director);
    Director update(Director director);
    void delete(Long id);
}
