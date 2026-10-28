package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipStorage;
    private final EventDbStorage eventStorage;

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User user) {
        return userStorage.update(user);
    }

    public void delete(Long id) {
        userStorage.delete(id);
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public List<User> findAll() {
        return List.copyOf(userStorage.findAll());
    }

    public List<Event> getFeed(Long userId) {
        findById(userId);
        return eventStorage.getFeed(userId);
    }

    public void addFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        friendshipStorage.addFriend(userId, friendId);
        Event event = new Event();
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(userId);
        event.setEventType("FRIEND");
        event.setOperation("ADD");
        event.setEntityId(friendId);
        eventStorage.create(event);
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        friendshipStorage.removeFriend(userId, friendId);

        Event event = new Event();
        event.setTimestamp(System.currentTimeMillis());
        event.setUserId(userId);
        event.setEventType("FRIEND");
        event.setOperation("REMOVE");
        event.setEntityId(friendId);
        eventStorage.create(event);
    }

    public List<User> getFriends(Long userId) {
        findById(userId);
        return friendshipStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        findById(userId);
        findById(otherId);
        return friendshipStorage.getCommonFriends(userId, otherId);
    }
}