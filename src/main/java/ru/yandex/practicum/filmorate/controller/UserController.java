package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        log.info("POST /users: создание пользователя {}", user);
        return userService.create(user);
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        log.info("PUT /users: обновление пользователя {}", user);
        return userService.update(user);
    }

    @GetMapping
    public List<User> findAll() {
        log.info("GET /users: получение всех пользователей");
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        log.info("GET /users/{}: получение пользователя по id", id);
        return userService.findById(id);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("PUT /users/{}/friends/{}: добавление в друзья", id, friendId);
        userService.addFriend(id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("DELETE /users/{}/friends/{}: удаление из друзей", id, friendId);
        userService.removeFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        log.info("GET /users/{}/friends: получение списка друзей", id);
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("GET /users/{}/friends/common/{}: получение общих друзей", id, otherId);
        return userService.getCommonFriends(id, otherId);
    }
}