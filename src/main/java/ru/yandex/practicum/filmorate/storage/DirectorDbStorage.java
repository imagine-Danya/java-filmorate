package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mappers.DirectorRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DirectorDbStorage implements DirectorStorage {

    private static final String FIND_ALL_SQL =
            "SELECT director_id, name, surname FROM directors";
    private static final String FIND_BY_ID_SQL =
            "SELECT director_id, name, surname FROM directors WHERE director_id = ?";
    private static final String ADD_DIRECTOR_SQL =
            "INSERT INTO directors (name, surname) VALUES (?, ?)";
    private static final String UPDATE_DIRECTOR_SQL =
            "UPDATE directors SET name = ?, surname = ? WHERE director_id = ?";
    private static final String DELETE_DIRECTOR_SQL =
            "DELETE FROM directors WHERE director_id = ?";

    private final JdbcTemplate jdbc;
    private final DirectorRowMapper directorRowMapper;

    @Override
    public Optional<Director> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            Director director = jdbc.queryForObject(FIND_BY_ID_SQL, directorRowMapper, id);
            return Optional.of(director);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Режиссёр с id {} не найден", id);
            return Optional.empty();
        }
    }

    @Override
    public List<Director> findAll() {
        return jdbc.query(FIND_ALL_SQL, directorRowMapper);
    }

    @Transactional
    @Override
    public Director create(Director director) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    ADD_DIRECTOR_SQL, new String[]{"director_id"});
            statement.setString(1, director.getName());
            statement.setString(2, director.getSurname());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException(
                    "Не удалось получить сгенерированный director_id для режиссёра: "
                            + director.getName());
        }
        director.setId(key.longValue());
        log.debug("Режиссёр создан в БД: {}", director);
        return director;
    }

    @Transactional
    @Override
    public Director update(Director director) {
        int rows = jdbc.update(UPDATE_DIRECTOR_SQL,
                director.getName(),
                director.getSurname(),
                director.getId());
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + director.getId() + " не найден");
        }
        log.debug("Режиссёр обновлён в БД: {}", director);
        return director;
    }

    @Transactional
    @Override
    public void delete(Long id) {
        int rows = jdbc.update(DELETE_DIRECTOR_SQL, id);
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден");
        }
        log.debug("Режиссёр с id {} удалён из БД", id);
    }
}