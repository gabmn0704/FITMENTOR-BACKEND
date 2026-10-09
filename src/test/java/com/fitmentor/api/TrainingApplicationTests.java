package com.fitmentor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TrainingApplicationTests {
    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate database;

    @Test
    void healthEndpointIsReady() {
        ResponseEntity<Map> response = http.getForEntity("/api/health", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "ok");
    }

    @Test
    void startupCreatesTrainingSchema() {
        Integer tableCount = database.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME IN ('USERS', 'TRAINING_SESSIONS')",
            Integer.class
        );

        assertThat(tableCount).isEqualTo(2);
    }
}