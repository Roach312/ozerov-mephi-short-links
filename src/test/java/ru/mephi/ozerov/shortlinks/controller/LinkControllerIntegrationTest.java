package ru.mephi.ozerov.shortlinks.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREATE_BODY = """
            {"originalUrl": "https://www.baeldung.com/java-9-http-client"}
            """;
    private static final String CREATE_BODY_WITH_LIMIT = """
            {"originalUrl": "https://example.com", "clickLimit": 5}
            """;

    @Test
    void create_withoutUserId_returns201AndGeneratesUserId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-User-Id"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.link.shortCode").exists())
                .andExpect(jsonPath("$.link.originalUrl").value("https://www.baeldung.com/java-9-http-client"))
                .andExpect(jsonPath("$.link.shortUrl").value(containsString("http://localhost:8080/")))
                .andReturn();

        String userId = result.getResponse().getHeader("X-User-Id");
        assert userId != null;
    }

    @Test
    void create_withUserId_returns201AndUsesProvidedUserId() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/links")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY_WITH_LIMIT))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-User-Id", userId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.link.clickLimit").value(5))
                .andExpect(jsonPath("$.link.clicksCount").value(0));
    }

    @Test
    void create_invalidUrl_returns400() throws Exception {
        String invalidBody = "{\"originalUrl\": \"not-a-url\"}";
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_withoutUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/links"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("X-User-Id")));
    }

    @Test
    void list_withUserId_returns200AndList() throws Exception {
        UUID userId = UUID.randomUUID();
        // Создаём ссылку
        mockMvc.perform(post("/api/links")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/links").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getOne_withValidIdAndOwner_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        Long linkId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").get("id").asLong();

        mockMvc.perform(get("/api/links/" + linkId).header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(linkId))
                .andExpect(jsonPath("$.originalUrl").value("https://www.baeldung.com/java-9-http-client"));
    }

    @Test
    void getOne_withWrongUserId_returns404() throws Exception {
        UUID ownerId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        Long linkId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").get("id").asLong();

        mockMvc.perform(get("/api/links/" + linkId).header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_withOwner_updatesLink() throws Exception {
        UUID userId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        Long linkId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").get("id").asLong();

        String updateBody = "{\"originalUrl\": \"https://updated.com\", \"clickLimit\": 3}";
        mockMvc.perform(put("/api/links/" + linkId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://updated.com"))
                .andExpect(jsonPath("$.clickLimit").value(3));
    }

    @Test
    void delete_withOwner_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/api/links")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        Long linkId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("link").get("id").asLong();

        mockMvc.perform(delete("/api/links/" + linkId).header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/links/" + linkId).header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}
