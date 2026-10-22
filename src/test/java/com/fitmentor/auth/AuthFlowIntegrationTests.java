package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private GoogleTokenVerifier googleVerifier;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void guestLoginCreatesGuestWithSyntheticEmailAndToken() throws Exception {
        JsonNode body = login("/api/auth/guest", "{\"name\":\"Ana\"}");

        assertThat(body.path("token").asText()).isNotBlank();
        JsonNode user = body.path("user");
        assertThat(user.path("guest").asBoolean()).isTrue();
        assertThat(user.path("name").asText()).isEqualTo("Ana");
        assertThat(user.path("email").asText()).matches("guest-[0-9a-f-]{36}@guest\\.fitmentor\\.local");
        assertThat(user.path("level").asText()).isEqualTo("BEGINNER");
    }

    @Test
    void guestLoginWithoutBodyIsAccepted() throws Exception {
        JsonNode body = login("/api/auth/guest", null);

        assertThat(body.path("user").path("name").asText()).isEqualTo("Invitado");
    }

    @Test
    void protectedEndpointsRequireBearerToken() throws Exception {
        MvcResult result = mvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("unauthorized"))
            .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("trace", "Exception");
        mvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer invalid.token.value"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsAuthenticatedUserAndPatchUpdatesProfile() throws Exception {
        String token = login("/api/auth/guest", "{\"name\":\"Luis\"}").path("token").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Luis"));

        mvc.perform(patch("/api/me").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"level\":\"ADVANCED\",\"goal\":\"fuerza\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.level").value("ADVANCED"))
            .andExpect(jsonPath("$.goal").value("fuerza"));
    }

    @Test
    void patchRejectsInvalidValues() throws Exception {
        String token = login("/api/auth/guest", null).path("token").asText();

        mvc.perform(patch("/api/me").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
        mvc.perform(patch("/api/me").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"level\":\"GODLIKE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void googleLoginCreatesThenReusesUser() throws Exception {
        when(googleVerifier.verify(eq("good-credential"))).thenReturn(
            new GoogleProfile("google-sub-1", "pat-" + System.nanoTime() + "@example.com", true, "Pat", "https://img/p.png"));

        JsonNode first = login("/api/auth/google", "{\"credential\":\"good-credential\"}");
        JsonNode second = login("/api/auth/google", "{\"credential\":\"good-credential\"}");

        assertThat(first.path("user").path("guest").asBoolean()).isFalse();
        assertThat(second.path("user").path("id").asText()).isEqualTo(first.path("user").path("id").asText());
        assertThat(second.path("user").path("picture").asText()).isEqualTo("https://img/p.png");
    }

    @Test
    void invalidGoogleCredentialIsUnauthorized() throws Exception {
        when(googleVerifier.verify(anyString())).thenThrow(
            new ApiException(HttpStatus.UNAUTHORIZED, "invalid_google_token", "The Google credential is not valid."));

        mvc.perform(post("/api/auth/google").contentType("application/json")
                .content("{\"credential\":\"forged\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_google_token"));
    }

    @Test
    void missingGoogleCredentialIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/google").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    private JsonNode login(String path, String body) throws Exception {
        var request = post(path).contentType("application/json");
        if (body != null) {
            request.content(body);
        }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
