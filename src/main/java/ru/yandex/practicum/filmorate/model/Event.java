package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("entityId")
    private Long entityId;
}