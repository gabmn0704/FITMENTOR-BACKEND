package com.fitmentor.coach;

import com.fitmentor.ai.AiServiceClient;
import com.fitmentor.coach.CoachService.CoachAnswer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoachServiceTests {

    @Test
    void usesAiAnswerWhenAvailable() {
        AiServiceClient ai = mock(AiServiceClient.class);
        when(ai.coach(anyString(), isNull())).thenReturn(Optional.of("Descansa 60 segundos."));

        CoachAnswer answer = new CoachService(ai).ask("¿Cuánto descanso?", null);

        assertThat(answer).isEqualTo(new CoachAnswer("Descansa 60 segundos.", "ai"));
    }

    @Test
    void fallsBackToRulesWhenAiIsDown() {
        AiServiceClient ai = mock(AiServiceClient.class);
        when(ai.coach(anyString(), isNull())).thenReturn(Optional.empty());

        CoachAnswer answer = new CoachService(ai).ask("Siento dolor en la rodilla", null);

        assertThat(answer.source()).isEqualTo("rules");
        assertThat(answer.answer()).contains("profesional");
    }

    @Test
    void rulesMatchWithoutAccentsAndDefaultAnswerExists() {
        RuleBasedCoach rules = new RuleBasedCoach();

        assertThat(rules.answer("¿Qué técnica uso en la plancha?")).contains("espalda neutra");
        assertThat(rules.answer("¿Cuántas repeticiones?")).contains("repetición");
        assertThat(rules.answer("hola")).isEqualTo(RuleBasedCoach.DEFAULT_ANSWER);
    }
}
