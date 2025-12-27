package com.zzk.interfaces.controller;

import com.alibaba.fastjson2.JSON;
import com.zzk.application.service.UserAppService;
import com.zzk.domain.model.entity.User;
import com.zzk.interfaces.dto.request.LoginRequest;
import com.zzk.interfaces.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAppService userAppService;

    @Test
    @DisplayName("POST /api/users/register - 注册成功")
    void register_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test123456");
        request.setEmail("test@example.com");

        User mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(userAppService.register(anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/users/register - 用户名为空返回错误")
    void register_EmptyUsername() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("Test123456");

        // When & Then
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/users/login - 登录成功")
    void login_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("Test123456");

        when(userAppService.login(eq("testuser"), eq("Test123456")))
                .thenReturn("mock-jwt-token");
        
        User mockUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();
        when(userAppService.getUserByUsername("testuser"))
                .thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("GET /api/users/current - 未登录返回 401")
    void getCurrentUser_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/check-username - 用户名可用")
    void checkUsername_Available() throws Exception {
        // Given
        when(userAppService.isUsernameAvailable("newuser")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/users/check-username")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("GET /api/users/check-username - 用户名已存在")
    void checkUsername_Exists() throws Exception {
        // Given
        when(userAppService.isUsernameAvailable("existinguser")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/users/check-username")
                        .param("username", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));
    }
}
