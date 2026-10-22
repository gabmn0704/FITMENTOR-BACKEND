package com.fitmentor.training;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Body of {@code POST /api/sessions}. */
public record SessionRequest(
    @NotBlank(message = "exercise is required")
    @Size(max = 60, message = "exercise must be at most 60 characters")
    String exercise,

    @NotNull(message = "reps is required")
    @Min(value = 0, message = "reps must be >= 0")
    @Max(value = 100000, message = "reps is too large")
    Integer reps,

    @NotNull(message = "accuracy is required")
    @DecimalMin(value = "0", message = "accuracy must be between 0 and 100")
    @DecimalMax(value = "100", message = "accuracy must be between 0 and 100")
    Double accuracy,

    @NotNull(message = "durationSeconds is required")
    @Min(value = 0, message = "durationSeconds must be >= 0")
    @Max(value = 86400, message = "durationSeconds must be at most 24 hours")
    Integer durationSeconds,

    @Size(max = 20, message = "avgAngles can contain at most 20 joints")
    Map<@NotBlank @Size(max = 40, message = "avgAngles keys must be at most 40 characters") String,
        @NotNull @DecimalMin(value = "-360", message = "angles must be between -360 and 360")
        @DecimalMax(value = "360", message = "angles must be between -360 and 360") Double> avgAngles
) {
}
