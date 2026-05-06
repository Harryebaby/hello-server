package org.example.helloserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.helloserver.entity.User;
import org.example.helloserver.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelloServerApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper.delete(null);
    }

    @Test
    void registerShouldPersistUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("alice", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").value("注册成功！"));

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, "alice");
        User dbUser = userMapper.selectOne(queryWrapper);
        assertThat(dbUser).isNotNull();
        assertThat(dbUser.getUsername()).isEqualTo("alice");
    }

    @Test
    void duplicateRegisterShouldFail() throws Exception {
        userMapper.insert(new User(null, "alice", "123456"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("alice", "654321")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.msg").value("该用户名已被注册"));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        userMapper.insert(new User(null, "alice", "123456"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("alice", "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.startsWith("eyJ")));
    }

    @Test
    void wrongPasswordShouldFail() throws Exception {
        userMapper.insert(new User(null, "alice", "123456"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("alice", "bad-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003))
                .andExpect(jsonPath("$.msg").value("账号或密码错误"));
    }

    @Test
    void otherApiShouldRequireAuthentication() throws Exception {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("123456");
        userMapper.insert(user);

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedApiShouldAllowValidJwt() throws Exception {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("123456");
        userMapper.insert(user);

        String response = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("alice", "123456")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).get("data").asText();

        mockMvc.perform(get("/api/users/{id}", user.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void corsPreflightShouldBeAllowed() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    private String jsonBody(String username, String password) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        return objectMapper.writeValueAsString(body);
    }
}
