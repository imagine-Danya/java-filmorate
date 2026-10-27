package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.storage.EventDbStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipStorage;
    private final EventDbStorage eventStorage;

    public User create(User user) {
        log.info("Создание пользователя: {}", user.getLogin());
        return userStorage.create(user);
    }

    public User update(User user) {
        log.info("Обновление пользователя: {}", user.getId());
        return userStorage.update(user);
    }

    public void delete(Long id) {
        log.info("Удаление пользователя: {}", id);
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

        Event eventForUser = new Event();
        eventForUser.setTimestamp(System.currentTimeMillis());
        eventForUser.setUserId(userId);
        eventForUser.setEventType("FRIEND");
        eventForUser.setOperation("ADD");
        eventForUser.setEntityId(friendId);
        eventStorage.create(eventForUser);

        Event eventForFriend = new Event();
        eventForFriend.setTimestamp(System.currentTimeMillis());
        eventForFriend.setUserId(friendId);
        eventForFriend.setEventType("FRIEND");
        eventForFriend.setOperation("ADD");
        eventForFriend.setEntityId(userId);
        eventStorage.create(eventForFriend);

        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        friendshipStorage.removeFriend(userId, friendId);

        Event eventForUser = new Event();
        eventForUser.setTimestamp(System.currentTimeMillis());
        eventForUser.setUserId(userId);
        eventForUser.setEventType("FRIEND");
        eventForUser.setOperation("REMOVE");
        eventForUser.setEntityId(friendId);
        eventStorage.create(eventForUser);
        
        Event eventForFriend = new Event();
        eventForFriend.setTimestamp(System.currentTimeMillis());
        eventForFriend.setUserId(friendId);
        eventForFriend.setEventType("FRIEND");
        eventForFriend.setOperation("REMOVE");
        eventForFriend.setEntityId(userId);
        eventStorage.create(eventForFriend);

        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
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