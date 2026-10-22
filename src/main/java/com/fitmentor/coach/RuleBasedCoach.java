package com.fitmentor.coach;

import com.fitmentor.domain.exercise.ExerciseCatalog;

import java.util.List;

/**
 * Offline answers used when the AI coach is unavailable. Each rule is a list of keywords
 * (matched without accents and in lowercase) and an answer; the first matching rule wins.
 */
public final class RuleBasedCoach {

    static final String DEFAULT_ANSWER =
        "Concéntrate en la técnica: movimiento controlado, respiración constante y descanso adecuado.";

    private static final List<Rule> RULES = List.of(
        new Rule(List.of("dolor", "duele", "lesion"),
            "Si sientes dolor, detente y consulta a un profesional. No fuerces el movimiento."),
        new Rule(List.of("descans", "rest"),
            "Descansa entre 60 y 90 segundos entre series y ajusta el tiempo a tu respiración."),
        new Rule(List.of("cuant", "repet", "series"),
            "Empieza con series que completes con buena técnica y sube una repetición por semana."),
        new Rule(List.of("tecnica", "forma", "postura"),
            "Prioriza la forma: espalda neutra, rodillas alineadas con los pies y control en la bajada."),
        new Rule(List.of("progres", "siguiente", "avanz"),
            "Cuando domines la técnica en tres series seguidas, pasa al siguiente ejercicio de la progresión.")
    );

    public String answer(String question) {
        String normalized = ExerciseCatalog.normalize(question);
        return RULES.stream()
            .filter(rule -> rule.matches(normalized))
            .findFirst()
            .map(Rule::answer)
            .orElse(DEFAULT_ANSWER);
    }

    private record Rule(List<String> keywords, String answer) {
        boolean matches(String normalizedQuestion) {
            return keywords.stream().anyMatch(normalizedQuestion::contains);
        }
    }
}
