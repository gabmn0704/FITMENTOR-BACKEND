package com.fitmentor.coach;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Body profile and plan numbers the web app sends with a coach question so the answer can be personal.
 *
 * <p>Only enumerations and bounded numbers are accepted, never free text, so the profile cannot carry
 * instructions to the model. It is read as camelCase from the client and forwarded as snake_case to the AI service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoachProfile(
    @Pattern(regexp = "lose_fat|build_muscle|tone|endurance|health", message = "unknown goal")
    String goal,

    @JsonProperty("weight_kg") @JsonAlias("weightKg")
    @DecimalMin("30") @DecimalMax("250")
    Double weightKg,

    @JsonProperty("height_cm") @JsonAlias("heightCm")
    @DecimalMin("120") @DecimalMax("230")
    Double heightCm,

    @Min(14) @Max(90)
    Integer age,

    @Pattern(regexp = "female|male", message = "unknown sex")
    String sex,

    @JsonProperty("target_weight_kg") @JsonAlias("targetWeightKg")
    @DecimalMin("30") @DecimalMax("250")
    Double targetWeightKg,

    @Min(1) @Max(104)
    Integer weeks,

    @Min(1) @Max(104)
    Integer week,

    @JsonProperty("days_per_week") @JsonAlias("daysPerWeek")
    @Min(1) @Max(7)
    Integer daysPerWeek,

    @Pattern(regexp = "omnivore|vegetarian", message = "unknown diet")
    String diet,

    @Min(800) @Max(6000)
    Integer kcal,

    @JsonProperty("protein_g") @JsonAlias("proteinG")
    @Min(0) @Max(400)
    Integer proteinG
) {
}
