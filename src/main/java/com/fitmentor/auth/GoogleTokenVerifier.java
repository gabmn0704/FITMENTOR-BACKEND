package com.fitmentor.auth;

/**
 * Port: verifies a Google ID token and returns the identity it carries. The production
 * implementation is {@link GoogleIdTokenVerifierAdapter}; tests replace it.
 */
public interface GoogleTokenVerifier {

    /**
     * @throws com.fitmentor.api.error.ApiException 401 when the token is invalid,
     *         503 when Google cannot be reached or the client id is not configured.
     */
    GoogleProfile verify(String credential);
}
