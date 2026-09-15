package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserStorage userStorage;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andDo(print());
    }

    @Test
    void createUser_withInvalidEmail_shouldReturnBadRequest() throws Exception {
        user.setEmail("invalid-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void createUser_withSpacesInLogin_shouldReturnBadRequest() throws Exception {
        user.setLogin("test user");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void createUser_withFutureBirthday_shouldReturnBadRequest() throws Exception {
        user.setBirthday(LocalDate.now().plusDays(1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() throws Exception {
        User created = userStorage.create(user);
        created.setName("Updated Name");

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(created)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andDo(print());
    }

    @Test
    void findAllUsers_shouldReturnListOfUsers() throws Exception {
        userStorage.create(user);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.login == 'testuser')]").exists())
                .andDo(print());
    }

    @Test
    void findById_shouldReturnUser() throws Exception {
        User created = userStorage.create(user);

        mockMvc.perform(get("/users/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.login").value("testuser"))
                .andDo(print());
    }

    @Test
    void findById_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andDo(print());
    }

    @Test
    void addFriend_shouldReturnOk() throws Exception {
        User user1 = userStorage.create(user);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User user2Created = userStorage.create(user2);

        mockMvc.perform(put("/users/" + user1.getId() + "/friends/" + user2Created.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void removeFriend_shouldReturnOk() throws Exception {
        User user1 = userStorage.create(user);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User user2Created = userStorage.create(user2);

        user1.getFriends().add(user2Created.getId());
        user2Created.getFriends().add(user1.getId());
        userStorage.update(user1);
        userStorage.update(user2Created);

        mockMvc.perform(delete("/users/" + user1.getId() + "/friends/" + user2Created.getId()))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void getFriends_shouldReturnListOfFriends() throws Exception {
        User user1 = userStorage.create(user);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User user2Created = userStorage.create(user2);

        user1.getFriends().add(user2Created.getId());
        user2Created.getFriends().add(user1.getId());
        userStorage.update(user1);
        userStorage.update(user2Created);

        mockMvc.perform(get("/users/" + user1.getId() + "/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }

    @Test
    void getCommonFriends_shouldReturnListOfCommonFriends() throws Exception {
        User user1 = userStorage.create(user);

        User user2 = new User();
        user2.setEmail("friend@example.com");
        user2.setLogin("friend");
        user2.setBirthday(LocalDate.of(1991, 1, 1));
        User user2Created = userStorage.create(user2);

        User commonFriend = new User();
        commonFriend.setEmail("common@example.com");
        commonFriend.setLogin("common");
        commonFriend.setBirthday(LocalDate.of(1992, 1, 1));
        User commonFriendCreated = userStorage.create(commonFriend);

        user1.getFriends().add(user2Created.getId());
        user1.getFriends().add(commonFriendCreated.getId());
        user2Created.getFriends().add(commonFriendCreated.getId());
        userStorage.update(user1);
        userStorage.update(user2Created);

        mockMvc.perform(get("/users/" + user1.getId() + "/friends/common/" + user2Created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andDo(print());
    }
}