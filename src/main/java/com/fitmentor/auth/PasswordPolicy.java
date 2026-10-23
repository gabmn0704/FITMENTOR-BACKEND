package com.fitmentor.auth;

import java.nio.charset.StandardCharsets;

/**
 * Password limits shared by the register and login requests. BCrypt only uses the first 72 bytes
 * and rejects longer input, so the limit is counted in UTF-8 bytes rather than characters.
 */
final class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 72;
    static final int BCRYPT_MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** True when the password fits BCrypt's byte limit. A null password is left to @NotBlank. */
    static boolean fitsBcryptLimit(String password) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }
}
