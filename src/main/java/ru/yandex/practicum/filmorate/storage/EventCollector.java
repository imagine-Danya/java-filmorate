package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class EventCollector {

    private final Map<Long, List<Event>> userEvents = new ConcurrentHashMap<>();
    private final AtomicLong eventIdGenerator = new AtomicLong(1);

    public void addEvent(Long userId, String eventType, String operation, Long entityId) {
        Event event = new Event();
        event.setEventId(eventIdGenerator.getAndIncrement());
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setEntityId(entityId);

        userEvents.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(event);
    }

    public List<Event> getFeed(Long userId) {
        List<Event> events = userEvents.getOrDefault(userId, new ArrayList<>());
        return events.stream()
                .sorted(Comparator.comparing(Event::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public void clear() {
        userEvents.clear();
        eventIdGenerator.set(1);
    }
}