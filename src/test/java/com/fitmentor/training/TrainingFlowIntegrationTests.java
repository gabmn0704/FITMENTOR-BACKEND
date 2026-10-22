package com.fitmentor.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrainingFlowIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void sessionsStatsRoutineAndAchievementsFollowTheContract() throws Exception {
        String token = guestToken("Marta");

        ResultActions squat = session(token, "{\"exercise\":\"squat\",\"reps\":10,\"accuracy\":92,"
            + "\"durationSeconds\":1800,\"avgAngles\":{\"left_knee\":95.5}}");
        squat.andExpect(status().isCreated())
            .andExpect(jsonPath("$.exercise").value("squat"))
            .andExpect(jsonPath("$.calories").value(175.0))
            .andExpect(jsonPath("$.avgAngles.left_knee").value(95.5));
        session(token, "{\"exercise\":\"push_up\",\"reps\":20,\"accuracy\":80,\"durationSeconds\":600}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.calories").value(93.33));

        mvc.perform(get("/api/sessions").param("limit", "1").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].exercise").value("push_up"));

        mvc.perform(get("/api/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSessions").value(2))
            .andExpect(jsonPath("$.totalReps").value(30))
            .andExpect(jsonPath("$.avgAccuracy").value(86.0))
            .andExpect(jsonPath("$.totalCalories").value(268.33))
            .andExpect(jsonPath("$.byExercise.length()").value(2))
            .andExpect(jsonPath("$.weekly.length()").value(7))
            .andExpect(jsonPath("$.level").value("BEGINNER"));

        mvc.perform(get("/api/routine").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.level").value("BEGINNER"))
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].exercise").exists())
            .andExpect(jsonPath("$.restSeconds").isNumber());

        mvc.perform(get("/api/achievements").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='first_session')].unlocked").value(true))
            .andExpect(jsonPath("$[?(@.id=='reps_100')].progress").value(30));
    }

    @Test
    void usersOnlySeeTheirOwnData() throws Exception {
        String owner = guestToken("Owner");
        String other = guestToken("Other");
        session(owner, "{\"exercise\":\"plank\",\"reps\":1,\"accuracy\":50,\"durationSeconds\":60}")
            .andExpect(status().isCreated());

        mvc.perform(get("/api/sessions").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/stats").header("Authorization", "Bearer " + other))
            .andExpect(jsonPath("$.totalSessions").value(0));
    }

    @Test
    void invalidSessionsAreRejectedWithErrorBody() throws Exception {
        String token = guestToken("Validation");

        session(token, "{\"exercise\":\"burpee\",\"reps\":1,\"accuracy\":50,\"durationSeconds\":60}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("unknown_exercise"));
        session(token, "{\"exercise\":\"squat\",\"reps\":-1,\"accuracy\":50,\"durationSeconds\":60}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
        session(token, "{\"exercise\":\"squat\",\"reps\":1,\"accuracy\":101,\"durationSeconds\":60}")
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/sessions").param("limit", "0").header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    @Test
    void exerciseCatalogSearchAndProgressions() throws Exception {
        String token = guestToken("Catalog");

        mvc.perform(get("/api/exercises").param("q", "PUSH").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("push_up"));
        mvc.perform(get("/api/exercises/next").param("from", "plank").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].distance").value(1));
        mvc.perform(get("/api/exercises/next").param("from", "burpee").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    private String guestToken(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/guest").contentType("application/json")
                .content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.path("token").asText();
    }

    private ResultActions session(String token, String body) throws Exception {
        return mvc.perform(post("/api/sessions").header("Authorization", "Bearer " + token)
            .contentType("application/json").content(body));
    }
}
