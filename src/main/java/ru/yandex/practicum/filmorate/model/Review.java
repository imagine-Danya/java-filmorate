package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Review {
    @JsonProperty("reviewId")
    private Long reviewId;

    @NotBlank(message = "Содержание отзыва не может быть пустым")
    @Size(max = 500, message = "Максимальная длина отзыва — 500 символов")
    @JsonProperty("content")
    private String content;

    @NotNull(message = "Тип отзыва должен быть указан")
    @JsonProperty("isPositive")
    private Boolean isPositive;

    @NotNull(message = "ID пользователя должен быть указан")
    @JsonProperty("userId")
    private Long userId;

    @NotNull(message = "ID фильма должен быть указан")
    @JsonProperty("filmId")
    private Long filmId;

    @JsonProperty("useful")
    private Integer useful = 0;
}