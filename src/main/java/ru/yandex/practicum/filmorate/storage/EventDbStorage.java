package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventDbStorage {

    private final JdbcTemplate jdbc;

    public Event create(Event event) {
        String sql = "INSERT INTO events (timestamp, user_id, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        jdbc.update(sql,
                event.getTimestamp(),
                event.getUserId(),
                event.getEventType(),
                event.getOperation(),
                event.getEntityId());

        log.debug("Событие создано: {}", event);
        return event;
    }

    public List<Event> getFeed(Long userId) {
        String sql = "SELECT event_id, timestamp, user_id, event_type, operation, entity_id " +
                "FROM events WHERE user_id = ? ORDER BY timestamp DESC";
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