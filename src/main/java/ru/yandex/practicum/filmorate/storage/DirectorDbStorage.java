package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbc;

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (name, surname) VALUES (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"director_id"});
            ps.setString(1, director.getName());
            ps.setString(2, director.getSurname());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            director.setId(key.longValue());
        }

        log.debug("Режиссёр создан: {}", director);
        return director;
    }

    @Override
    public Director update(Director director) {
        String sql = "UPDATE directors SET name = ?, surname = ? WHERE director_id = ?";
        int rows = jdbc.update(sql, director.getName(), director.getSurname(), director.getId());

        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + director.getId() + " не найден");
        }

        log.debug("Режиссёр обновлен: {}", director);
        return director;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        int rows = jdbc.update(sql, id);

        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден");
        }

        log.debug("Режиссёр удален: id={}", id);
    }

    @Override
    public Optional<Director> findById(Long id) {
        String sql = "SELECT director_id, name, surname FROM directors WHERE director_id = ?";
        List<Director> directors = jdbc.query(sql, directorMapper, id);
        return directors.isEmpty() ? Optional.empty() : Optional.of(directors.get(0));
    }

    @Override
    public List<Director> findAll() {
        String sql = "SELECT director_id, name, surname FROM directors ORDER BY director_id";
        return jdbc.query(sql, directorMapper);
    }

    private final RowMapper<Director> directorMapper = (rs, rowNum) -> {
        Director director = new Director();
        director.setId(rs.getLong("director_id"));
        director.setName(rs.getString("name"));
        director.setSurname(rs.getString("surname"));
        return director;
    };
}