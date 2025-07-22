package org.example.t1_hw4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.t1_hw4.dto.LoginDTO;
import org.example.t1_hw4.dto.RegisterDTO;
import org.example.t1_hw4.jwt.JwtTokenProvider;
import org.example.t1_hw4.mapper.UserMapper;
import org.example.t1_hw4.model.User;
import org.example.t1_hw4.model.UserRole;
import org.example.t1_hw4.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class T1Hw4ApplicationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @Transactional
    void testValidUserRegistration() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();

        registerDTO.setLogin("test");
        registerDTO.setPassword("testtest");
        registerDTO.setEmail("test@gmail.com");
        registerDTO.setRole(UserRole.ADMIN);

        String json = jacksonObjectMapper.writeValueAsString(registerDTO);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andDo(print());

        userRepository.deleteByLogin("test");
    }

    @Test
    @Transactional
    void testNotValidUserRegistration() throws Exception {
        String json = jacksonObjectMapper.writeValueAsString(
                Map.of(
                        "login", "",
                        "password", "",
                        "email", "",
                        "role", "ADMIN"
                )
        );

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    void testValidUserRegisterAndLogin() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();

        registerDTO.setLogin("admin");
        registerDTO.setPassword("12345678");
        registerDTO.setRole(UserRole.ADMIN);
        registerDTO.setEmail("admin@gmail.com");

        String json = jacksonObjectMapper.writeValueAsString(registerDTO);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        LoginDTO loginDTO = new LoginDTO();

        loginDTO.setLogin("admin");
        loginDTO.setPassword("12345678");

        String jsonLogin = jacksonObjectMapper.writeValueAsString(loginDTO);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andDo(print());

        userRepository.deleteByLogin("admin");
    }

    @Test
    @Transactional
    void testOnTokenRefresh() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setLogin("admin");
        registerDTO.setPassword("12345678");
        registerDTO.setRole(UserRole.ADMIN);
        registerDTO.setEmail("admin@gmail.com");

        String json = jacksonObjectMapper.writeValueAsString(registerDTO);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLogin("admin");
        loginDTO.setPassword("12345678");

        String jsonLogin = jacksonObjectMapper.writeValueAsString(loginDTO);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginJson = jacksonObjectMapper.readTree(loginResponse);

        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String refreshResponse = refreshResult.getResponse().getContentAsString();
        JsonNode refreshJson = jacksonObjectMapper.readTree(refreshResponse);

        String newAccessToken = refreshJson.get("accessToken").asText();

        assertNotEquals(accessToken, newAccessToken, "Access token should be refreshed and different");
        userRepository.deleteByLogin("admin");
    }

    @Test
    @Transactional
    public void testRegisterLoginLogoutAndRefreshBlockedToken() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setLogin("admin");
        registerDTO.setPassword("12345678");
        registerDTO.setRole(UserRole.ADMIN);
        registerDTO.setEmail("admin@gmail.com");

        String jsonRegister = jacksonObjectMapper.writeValueAsString(registerDTO);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonRegister))
                .andExpect(status().isCreated());

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setLogin("admin");
        loginDTO.setPassword("12345678");

        String jsonLogin = jacksonObjectMapper.writeValueAsString(loginDTO);

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode loginJson = jacksonObjectMapper.readTree(loginResponse);

        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        assertNotNull(accessToken);
        assertNotNull(refreshToken);


        mockMvc.perform(post("/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));

        mockMvc.perform(post("/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        userRepository.deleteByLogin("admin");

    }
}
