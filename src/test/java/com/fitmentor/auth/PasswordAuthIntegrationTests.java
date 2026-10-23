package com.fitmentor.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Email/password register and login over HTTP. Each test uses its own email: the context and DB are shared. */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordAuthIntegrationTests {

    private static final String PASSWORD = "clave-segura-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private GoogleTokenVerifier googleVerifier;

    @Test
    void registerCreatesLowercaseUserAndReturnsTokenWithoutHash() throws Exception {
        String email = uniqueEmail("Registro");
        MvcResult result = mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody("Ana", email.toUpperCase(), PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.guest").value(false))
            .andExpect(jsonPath("$.user.name").value("Ana"))
            .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("token").asText()).isNotBlank();
        assertThat(body.path("user").path("email").asText()).isEqualTo(email);
        assertThat(result.getResponse().getContentAsString())
            .doesNotContain(PASSWORD)
            .doesNotContain("password")
            .doesNotContain("$2a$")
            .doesNotContain("$2b$");
    }

    @Test
    void registeredTokenAuthenticatesProtectedEndpoints() throws Exception {
        String token = register("Luis", uniqueEmail("Token"), PASSWORD).path("token").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Luis"));
    }

    @Test
    void duplicateEmailIsConflictEvenWithDifferentCase() throws Exception {
        String email = uniqueEmail("Duplicado");
        register("Ana", email, PASSWORD);

        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody("Otra", email.toUpperCase(), "otra-clave-456")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("email_taken"))
            .andExpect(jsonPath("$.message").value("Ese correo ya está registrado"));
    }

    @Test
    void loginSucceedsWithRegisteredCredentials() throws Exception {
        String email = uniqueEmail("Login");
        String registeredId = register("Ana", email, PASSWORD).path("user").path("id").asText();

        JsonNode body = login(email.toUpperCase(), PASSWORD);

        assertThat(body.path("token").asText()).isNotBlank();
        assertThat(body.path("user").path("id").asText()).isEqualTo(registeredId);
    }

    @Test
    void wrongPasswordIsUnauthorizedWithGenericMessage() throws Exception {
        String email = uniqueEmail("Mala");
        register("Ana", email, PASSWORD);

        assertInvalidCredentials(email, "clave-incorrecta-999");
    }

    @Test
    void unknownEmailGetsTheSameGenericMessage() throws Exception {
        assertInvalidCredentials(uniqueEmail("Inexistente"), PASSWORD);
    }

    @Test
    void guestAccountCannotLoginWithPassword() throws Exception {
        JsonNode guest = json.readTree(mvc.perform(post("/api/auth/guest").contentType("application/json")
                .content("{\"name\":\"Invitado\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        String guestEmail = guest.path("user").path("email").asText();

        assertInvalidCredentials(guestEmail, PASSWORD);
    }

    @Test
    void googleAccountCannotLoginWithPassword() throws Exception {
        String email = uniqueEmail("GoogleSolo");
        when(googleVerifier.verify(eq("google-only-credential"))).thenReturn(
            new GoogleProfile("google-sub-" + System.nanoTime(), email, true, "Pat", null));
        mvc.perform(post("/api/auth/google").contentType("application/json")
                .content("{\"credential\":\"google-only-credential\"}"))
            .andExpect(status().isOk());

        assertInvalidCredentials(email, PASSWORD);
    }

    @Test
    void googleLinkRevokesPasswordSetBeforeByAnotherPerson() throws Exception {
        // Someone registers the victim's address with their own password (no email verification).
        String email = uniqueEmail("Takeover");
        register("Atacante", email, PASSWORD);

        // The real owner signs in with Google, which verifies the address and links the account.
        when(googleVerifier.verify(eq("takeover-credential"))).thenReturn(
            new GoogleProfile("google-sub-takeover-" + System.nanoTime(), email, true, "Dueño", null));
        mvc.perform(post("/api/auth/google").contentType("application/json")
                .content("{\"credential\":\"takeover-credential\"}"))
            .andExpect(status().isOk());

        // The previous password no longer grants access.
        assertInvalidCredentials(email, PASSWORD);
    }

    @Test
    void googleLinkInvalidatesTokensIssuedBeforeTheLink() throws Exception {
        String email = uniqueEmail("Tokens");
        String attackerToken = register("Atacante", email, PASSWORD).path("token").asText();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + attackerToken))
            .andExpect(status().isOk());

        // Google reports the address in upper case; the account is still found and linked.
        when(googleVerifier.verify(eq("tokens-credential"))).thenReturn(
            new GoogleProfile("google-sub-tokens-" + System.nanoTime(), email.toUpperCase(), true, "Dueño", null));
        String ownerToken = json.readTree(mvc.perform(post("/api/auth/google").contentType("application/json")
                .content("{\"credential\":\"tokens-credential\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).path("token").asText();

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + attackerToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("unauthorized"));
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody("Ana", uniqueEmail("Corta"), "1234567")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void passwordOver72BytesIsRejectedEvenIfUnder72Characters() throws Exception {
        String multibyte = "ñ".repeat(40);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody("Ana", uniqueEmail("Bytes"), multibyte)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"ana@example.com\",\"password\":\"" + multibyte + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void invalidEmailAndMissingFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody("Ana", "no-es-un-correo", PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"name\":\"\",\"email\":\"ana@example.com\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    private JsonNode register(String name, String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(registerBody(name, email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(loginBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void assertInvalidCredentials(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody(email, password)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid_credentials"))
            .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    private String registerBody(String name, String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("name", name, "email", email, "password", password));
    }

    private String loginBody(String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }

    private static String uniqueEmail(String tag) {
        return tag.toLowerCase() + "-" + System.nanoTime() + "@example.com";
    }
}
