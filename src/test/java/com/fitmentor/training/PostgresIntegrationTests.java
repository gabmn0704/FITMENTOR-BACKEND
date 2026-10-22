package com.fitmentor.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the production profile against a real PostgreSQL 16 database: the canonical schema
 * (db/schema.sql, copy of FITMENTOR-BD/init.sql) is applied at startup, Hibernate validates the
 * mapping and a guest flow writes jsonb angles. Enabled only when {@code FITMENTOR_TEST_PG_URL}
 * (a JDBC URL) is set, e.g. {@code jdbc:postgresql://127.0.0.1:5432/fitmentor}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("render")
@EnabledIfEnvironmentVariable(named = "FITMENTOR_TEST_PG_URL", matches = "jdbc:postgresql:.+")
class PostgresIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("FITMENTOR_TEST_PG_URL"));
        registry.add("spring.datasource.username", () -> env("FITMENTOR_TEST_PG_USER", "postgres"));
        registry.add("spring.datasource.password", () -> env("FITMENTOR_TEST_PG_PASSWORD", ""));
        registry.add("fitmentor.jwt.secret", () -> "integration-test-secret-with-more-than-32-chars");
        registry.add("ai.service-url", () -> "http://127.0.0.1:9");
    }

    @Test
    void guestSessionWithAnglesAndStatsWorkOnPostgres() throws Exception {
        String body = mvc.perform(post("/api/auth/guest").contentType("application/json").content("{\"name\":\"PG\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();
        String userId = json.readTree(body).get("user").get("id").asText();

        mvc.perform(post("/api/sessions").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"exercise\":\"squat\",\"reps\":12,\"accuracy\":87.456,\"durationSeconds\":600,"
                    + "\"avgAngles\":{\"knee\":96.5,\"hip\":110.25}}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.avgAngles.knee").value(96.5));

        mvc.perform(get("/api/sessions").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].avgAngles.hip").value(110.25))
            .andExpect(jsonPath("$[0].accuracy").value(87.46));

        mvc.perform(get("/api/stats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSessions").value(1))
            .andExpect(jsonPath("$.totalReps").value(12));

        mvc.perform(get("/api/routine").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/achievements").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        Map<String, Object> row = jdbc.queryForMap(
            "select jsonb_typeof(avg_angles) as kind, avg_angles->>'knee' as knee from training_sessions "
                + "where user_id = ?::uuid", userId);
        assertThat(row).containsEntry("kind", "object").containsEntry("knee", "96.5");
        assertThat(jdbc.queryForObject("select jsonb_typeof(items) from routine_plans where user_id = ?::uuid "
            + "limit 1", String.class, userId)).isEqualTo("array");
        assertThat(jdbc.queryForObject("select total_sessions from user_stats where user_id = ?::uuid",
            Long.class, userId)).isEqualTo(1L);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
