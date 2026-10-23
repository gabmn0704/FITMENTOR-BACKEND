package com.fitmentor.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTests {

    @Test
    void limitIsCountedInUtf8BytesNotCharacters() {
        String seventyTwoAscii = "a".repeat(72);
        String fortyMultibyte = "ñ".repeat(40); // 40 characters, 80 bytes

        assertThat(PasswordPolicy.fitsBcryptLimit(seventyTwoAscii)).isTrue();
        assertThat(PasswordPolicy.fitsBcryptLimit(fortyMultibyte)).isFalse();
        assertThat(PasswordPolicy.fitsBcryptLimit(null)).isTrue();
    }
}
