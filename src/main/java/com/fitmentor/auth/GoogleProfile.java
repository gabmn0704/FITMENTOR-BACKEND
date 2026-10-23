package com.fitmentor.auth;

/** Identity data taken from a verified Google ID token. */
public record GoogleProfile(String googleId, String email, boolean emailVerified, String name, String picture) {
}
