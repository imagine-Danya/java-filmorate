package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConflictException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectorService {

    private final DirectorStorage directorStorage;

    public Director create(Director director) {
        validate(director);
        try {
            Director created = directorStorage.create(director);
            log.info("Создан режиссёр: {}", created);
            return created;
        } catch (DuplicateKeyException e) {
            throw new ConflictException(
                    "Режиссёр " + director.getName() + " " + director.getSurname() + " уже существует");
        }
    }

    public Director findById(Long id) {
        return directorStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Режиссёр с id " + id + " не найден"));
    }

    public List<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director update(Director director) {
        if (director.getId() == null) {
            throw new ValidationException("id режиссёра обязателен для обновления");
        }
        validate(director);
        Director updated = directorStorage.update(director);
        log.info("Обновлён режиссёр: {}", updated);
        return updated;
    }

    public void delete(Long id) {
        directorStorage.delete(id);
        log.info("Удалён режиссёр с id {}", id);
    }

    private void validate(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        if (director.getSurname() == null || director.getSurname().isBlank()) {
            throw new ValidationException("Фамилия режиссёра не может быть пустой");
        }
    }
}
