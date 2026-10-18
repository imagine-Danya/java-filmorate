package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class MpaRatingDbStorage {

    private final JdbcTemplate jdbc;

    public Collection<MpaRating> findAll() {
        String sql = "SELECT mpa_rating_id, name, description FROM mpa_ratings ORDER BY mpa_rating_id";
        return jdbc.query(sql, mpaRatingMapper);
    }

    public Optional<MpaRating> findById(Long id) {
        String sql = "SELECT mpa_rating_id, name, description FROM mpa_ratings WHERE mpa_rating_id = ?";
        List<MpaRating> ratings = jdbc.query(sql, mpaRatingMapper, id);
        return ratings.isEmpty() ? Optional.empty() : Optional.of(ratings.get(0));
    }

    private final RowMapper<MpaRating> mpaRatingMapper = (rs, rowNum) -> {
        MpaRating rating = new MpaRating();
        rating.setId(rs.getLong("mpa_rating_id"));
        rating.setName(rs.getString("name"));
        rating.setDescription(rs.getString("description"));
        return rating;
    };
}
