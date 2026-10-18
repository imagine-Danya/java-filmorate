package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbc;

    public Collection<Genre> findAll() {
        String sql = "SELECT genre_id, name FROM genres ORDER BY genre_id";
        return jdbc.query(sql, genreMapper);
    }

    public Optional<Genre> findById(Long id) {
        String sql = "SELECT genre_id, name FROM genres WHERE genre_id = ?";
        List<Genre> genres = jdbc.query(sql, genreMapper, id);
        return genres.isEmpty() ? Optional.empty() : Optional.of(genres.get(0));
    }

    private final RowMapper<Genre> genreMapper = (rs, rowNum) -> {
        Genre genre = new Genre();
        genre.setId(rs.getLong("genre_id"));
        genre.setName(rs.getString("name"));
        return genre;
    };
}