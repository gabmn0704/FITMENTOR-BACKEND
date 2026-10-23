package com.fitmentor.api.error;

/**
 * Error body returned by every failing endpoint: {@code {"error": "...", "message": "..."}}.
 * Stack traces and internal details are never included.
 */
public record ApiError(String error, String message) {
}
