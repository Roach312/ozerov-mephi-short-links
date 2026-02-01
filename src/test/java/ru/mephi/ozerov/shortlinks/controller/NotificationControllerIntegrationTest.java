package ru.mephi.ozerov.shortlinks.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void list_withoutUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("X-User-Id")));
    }

    @Test
    void list_withUserId_returns200AndArray() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/notifications").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void whenClickLimitReached_notificationAppearsInList() throws Exception {
        UUID userId = UUID.randomUUID();
        String createBody = "{\"originalUrl\": \"https://notify-test.com\", \"clickLimit\": 1}";
        MvcResult createResult =
                mockMvc.perform(
                                post("/api/links")
                                        .header("X-User-Id", userId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(createBody))
                        .andExpect(status().isCreated())
                        .andReturn();
        String shortCode =
                objectMapper
                        .readTree(createResult.getResponse().getContentAsString())
                        .get("link")
                        .get("shortCode")
                        .asText();

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().isGone());

        mockMvc.perform(get("/api/notifications").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].type").value("CLICK_LIMIT_REACHED"))
                .andExpect(jsonPath("$[0].shortCode").value(shortCode))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void markAsRead_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        String createBody = "{\"originalUrl\": \"https://read-test.com\", \"clickLimit\": 1}";
        mockMvc.perform(
                        post("/api/links")
                                .header("X-User-Id", userId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody))
                .andExpect(status().isCreated());
        String shortCode =
                objectMapper
                        .readTree(
                                mockMvc.perform(
                                                get("/api/links")
                                                        .header("X-User-Id", userId.toString()))
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get(0)
                        .get("shortCode")
                        .asText();
        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection());

        MvcResult notifResult =
                mockMvc.perform(get("/api/notifications").header("X-User-Id", userId.toString()))
                        .andExpect(status().isOk())
                        .andReturn();
        Long notificationId =
                objectMapper
                        .readTree(notifResult.getResponse().getContentAsString())
                        .get(0)
                        .get("id")
                        .asLong();

        mockMvc.perform(
                        patch("/api/notifications/" + notificationId + "/read")
                                .header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(true));
    }
}
