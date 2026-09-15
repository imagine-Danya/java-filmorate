package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public User create(User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        log.debug("Пользователь создан: {}", user);
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }
        users.put(user.getId(), user);
        log.debug("Пользователь обновлен: {}", user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }
}