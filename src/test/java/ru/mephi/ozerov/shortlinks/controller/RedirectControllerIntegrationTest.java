package ru.mephi.ozerov.shortlinks.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedirectControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    /** При sendError() сообщение попадает в response.getErrorMessage(), а не в body. */
    private static ResultMatcher errorMessageContains(String substring) {
        return result -> {
            String msg = result.getResponse().getErrorMessage();
            assertNotNull(msg, "Ожидалось сообщение об ошибке в getErrorMessage()");
            assertTrue(
                    msg.contains(substring),
                    "Сообщение должно содержать: " + substring + ", получено: " + msg);
        };
    }

    @Test
    void redirect_withoutUserId_returns400() throws Exception {
        mockMvc.perform(get("/anyCode123"))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessageContains("X-User-Id"));
    }

    @Test
    void redirect_unknownShortCode_returns404() throws Exception {
        UUID anyUserId = UUID.randomUUID();
        mockMvc.perform(get("/unknownCode123").header("X-User-Id", anyUserId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(errorMessageContains("Ссылка не найдена"));
    }

    @Test
    void redirect_withWrongUserId_returns403() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String shortCode =
                createLinkAndGetShortCode(
                        ownerId, "{\"originalUrl\": \"https://www.example.com/target\"}");
        UUID otherUserId = UUID.randomUUID();

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", otherUserId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(errorMessageContains("Доступ запрещён"));
    }

    @Test
    void redirect_validShortLink_redirectsToOriginalUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        String shortCode =
                createLinkAndGetShortCode(
                        userId, "{\"originalUrl\": \"https://www.example.com/target\"}");

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.example.com/target"));
    }

    @Test
    void redirect_incrementsClickCount() throws Exception {
        UUID userId = UUID.randomUUID();
        String shortCode =
                createLinkAndGetShortCode(
                        userId, "{\"originalUrl\": \"https://example.com\", \"clickLimit\": 10}");

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection());

        MvcResult listResult =
                mockMvc.perform(get("/api/links").header("X-User-Id", userId.toString()))
                        .andExpect(status().isOk())
                        .andReturn();
        JsonNode links = objectMapper.readTree(listResult.getResponse().getContentAsString());
        for (JsonNode link : links) {
            if (shortCode.equals(link.get("shortCode").asText())) {
                org.junit.jupiter.api.Assertions.assertEquals(2, link.get("clicksCount").asInt());
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("Созданная ссылка не найдена в списке");
    }

    @Test
    void redirect_whenClickLimitReached_returns410() throws Exception {
        UUID userId = UUID.randomUUID();
        String shortCode =
                createLinkAndGetShortCode(
                        userId, "{\"originalUrl\": \"https://limit-test.com\", \"clickLimit\": 1}");

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().isGone())
                .andExpect(errorMessageContains("Лимит переходов"));
    }

    @Test
    void redirect_redirectsToStoredUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        // Валидный URL (www.example.com без схемы отклоняется @URL при создании)
        String shortCode =
                createLinkAndGetShortCode(userId, "{\"originalUrl\": \"https://www.example.com\"}");

        mockMvc.perform(get("/" + shortCode).header("X-User-Id", userId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.example.com"));
    }

    private String createLinkAndGetShortCode(UUID userId, String body) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/links")
                                        .header("X-User-Id", userId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode link = root.get("link");
        assertNotNull(link, "В ответе должен быть объект link");
        JsonNode shortCodeNode = link.get("shortCode");
        assertNotNull(shortCodeNode, "В link должен быть shortCode");
        return shortCodeNode.asText();
    }
}
