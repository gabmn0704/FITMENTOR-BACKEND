package com.fitmentor.auth;

import com.fitmentor.api.error.ApiException;
import com.fitmentor.config.FitmentorProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Adapter over Google's {@link GoogleIdTokenVerifier}: checks signature, expiry and that the
 * token was issued for our OAuth client id ({@code GOOGLE_CLIENT_ID}).
 */
@Component
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(FitmentorProperties properties) {
        this.clientId = properties.google().clientId();
        this.verifier = clientId == null || clientId.isBlank()
            ? null
            : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleProfile verify(String credential) {
        if (verifier == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "google_not_configured",
                "Google sign-in is not configured on this server.");
        }
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw invalid();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "google_unavailable",
                "Google could not be reached. Try again.");
        }
        if (token == null) {
            throw invalid();
        }
        GoogleIdToken.Payload payload = token.getPayload();
        return new GoogleProfile(
            payload.getSubject(),
            payload.getEmail(),
            Boolean.TRUE.equals(payload.getEmailVerified()),
            (String) payload.get("name"),
            (String) payload.get("picture"));
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_google_token", "The Google credential is not valid.");
    }
}
