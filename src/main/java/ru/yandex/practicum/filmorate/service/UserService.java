package ru.yandex.practicum.filmorate.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.storage.FriendshipDbStorage;

import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipStorage;

    public UserService(
            @Qualifier("userDbStorage") UserStorage userStorage,
            FriendshipDbStorage friendshipStorage) {
        this.userStorage = userStorage;
        this.friendshipStorage = friendshipStorage;
    }

    public User create(User user) {
        log.info("Создание пользователя: {}", user.getLogin());
        return userStorage.create(user);
    }

    public User update(User user) {
        log.info("Обновление пользователя: {}", user.getId());
        return userStorage.update(user);
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public List<User> findAll() {
        return List.copyOf(userStorage.findAll());
    }

    public void addFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        friendshipStorage.addFriend(userId, friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        findById(userId);
        findById(friendId);
        friendshipStorage.removeFriend(userId, friendId);
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