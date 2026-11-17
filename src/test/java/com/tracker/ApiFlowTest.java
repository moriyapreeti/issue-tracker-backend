package com.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ApiFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String login(String email, String password) throws Exception {
        String body = json.writeValueAsString(java.util.Map.of("email", email, "password", password));
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("token").asText();
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void developerCannotCreateProjectButManagerCan() throws Exception {
        String dev = login("dev@tracker.com", "Dev@1234");
        String manager = login("manager@tracker.com", "Manager@123");
        String body = json.writeValueAsString(java.util.Map.of("name", "Mobile App", "key", "MOB"));

        mvc.perform(post("/api/projects").header("Authorization", "Bearer " + dev)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/projects").header("Authorization", "Bearer " + manager)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
    }

    @Test
    void ticketLifecycle() throws Exception {
        String manager = login("manager@tracker.com", "Manager@123");
        String auth = "Bearer " + manager;

        JsonNode projects = json.readTree(mvc.perform(get("/api/projects").header("Authorization", auth))
                .andReturn().getResponse().getContentAsString());
        long projectId = projects.get(0).get("id").asLong();

        String create = json.writeValueAsString(java.util.Map.of("title", "Test ticket", "projectId", projectId, "priority", "HIGH"));
        String created = mvc.perform(post("/api/tickets").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long ticketId = json.readTree(created).get("id").asLong();

        String move = json.writeValueAsString(java.util.Map.of("status", "IN_PROGRESS"));
        String moved = mvc.perform(patch("/api/tickets/" + ticketId + "/status").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(move))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals("IN_PROGRESS", json.readTree(moved).get("status").asText());

        mvc.perform(get("/api/tickets").param("q", "test ticket").param("projectId", String.valueOf(projectId))
                .header("Authorization", auth)).andExpect(status().isOk());

        mvc.perform(delete("/api/tickets/" + ticketId).header("Authorization", auth)).andExpect(status().isNoContent());
    }
}
