package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.PreparedStatement;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventDbStorage {

    private final JdbcTemplate jdbc;

    public Event create(Event event) {
        String sql = "INSERT INTO events (timestamp, user_id, event_type, operation, entity_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"event_id"});
            ps.setLong(1, event.getTimestamp());
            ps.setLong(2, event.getUserId());
            ps.setString(3, event.getEventType());
            ps.setString(4, event.getOperation());
            ps.setLong(5, event.getEntityId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            event.setEventId(key.longValue());
        }
        return event;
    }

    public List<Event> getFeed(Long userId) {
        String sql = "SELECT event_id, timestamp, user_id, event_type, operation, entity_id " +
                "FROM events WHERE user_id = ? ORDER BY event_id DESC";
        return jdbc.query(sql, eventMapper, userId);
    }

    private final RowMapper<Event> eventMapper = (rs, rowNum) -> {
        Event event = new Event();
        event.setEventId(rs.getLong("event_id"));
        event.setTimestamp(rs.getLong("timestamp"));
        event.setUserId(rs.getLong("user_id"));
        event.setEventType(rs.getString("event_type"));
        event.setOperation(rs.getString("operation"));
        event.setEntityId(rs.getLong("entity_id"));
        return event;
    };
}