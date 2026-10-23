package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.config.FitmentorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Adapter over Google's verifier: errors are translated to the API's own exceptions. */
class GoogleIdTokenVerifierAdapterTests {

    @Test
    void withoutClientIdGoogleLoginIsUnavailable() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(properties(""));

        assertThatThrownBy(() -> adapter.verify("anything"))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                org.assertj.core.api.Assertions.assertThat(error.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                org.assertj.core.api.Assertions.assertThat(error.code()).isEqualTo("google_not_configured");
            });
    }

    @Test
    void malformedCredentialIsUnauthorized() {
        GoogleIdTokenVerifierAdapter adapter = new GoogleIdTokenVerifierAdapter(properties("client.apps.googleusercontent.com"));

        assertThatThrownBy(() -> adapter.verify("not-a-jwt"))
            .isInstanceOfSatisfying(ApiException.class, error ->
                org.assertj.core.api.Assertions.assertThat(error.code()).isEqualTo("invalid_google_token"));
    }

    private static FitmentorProperties properties(String clientId) {
        return new FitmentorProperties(null, new FitmentorProperties.Google(clientId), null, null);
    }
}
