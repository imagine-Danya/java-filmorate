package ru.yandex.practicum.filmorate.model;

import lombok.Getter;

@Getter
public enum FriendshipStatus {
    PENDING("Неподтверждённая"),
    CONFIRMED("Подтверждённая");

    private final String description;

    FriendshipStatus(String description) {
        this.description = description;
    }
}