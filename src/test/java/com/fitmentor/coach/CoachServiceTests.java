package com.fitmentor.coach;

import com.fitmentor.ai.AiServiceClient;
import com.fitmentor.ai.AiServiceClient.CoachContext;
import com.fitmentor.ai.AiServiceClient.CoachReply;
import com.fitmentor.coach.CoachService.CoachAnswer;
import com.fitmentor.training.SessionSummary;
import com.fitmentor.training.TrainingFacade;
import com.fitmentor.user.UserEntity;
import com.fitmentor.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoachServiceTests {

    private static final UUID USER = UUID.randomUUID();

    private final AiServiceClient ai = mock(AiServiceClient.class);
    private final UserService users = mock(UserService.class);
    private final TrainingFacade training = mock(TrainingFacade.class);
    private final CoachService coach = new CoachService(ai, users, training);

    @BeforeEach
    void user() {
        when(users.require(USER)).thenReturn(new UserEntity(USER, "Ana", "ana@example.com", false, Instant.EPOCH));
        when(training.recentSessions(USER, 1)).thenReturn(List.of());
    }

    @Test
    void claudeAnswerKeepsClaudeSource() {
        when(ai.coach(anyString(), any())).thenReturn(Optional.of(new CoachReply("Descansa 60 segundos.", "claude")));

        CoachAnswer answer = coach.ask(USER, "¿Cuánto descanso?");

        assertThat(answer).isEqualTo(new CoachAnswer("Descansa 60 segundos.", "claude"));
    }

    @Test
    void aiRulesAnswerIsReportedAsRules() {
        when(ai.coach(anyString(), any())).thenReturn(Optional.of(new CoachReply("Usa reglas.", "rules")));

        assertThat(coach.ask(USER, "hola").source()).isEqualTo("rules");
    }

    @Test
    void sendsServerBuiltContextFromLevelAndLastSession() {
        when(training.recentSessions(USER, 1)).thenReturn(List.of(SessionSummary.builder()
            .exercise("squat").reps(12).accuracy(81.5).createdAt(Instant.EPOCH).build()));
        when(ai.coach(anyString(), any())).thenReturn(Optional.empty());

        coach.ask(USER, "¿Cómo voy?");

        ArgumentCaptor<CoachContext> context = ArgumentCaptor.forClass(CoachContext.class);
        verify(ai).coach(eq("¿Cómo voy?"), context.capture());
        assertThat(context.getValue()).isEqualTo(new CoachContext("squat", 81.5, 12, "BEGINNER"));
    }

    @Test
    void fallsBackToRulesWhenAiIsDown() {
        when(ai.coach(anyString(), any())).thenReturn(Optional.empty());

        CoachAnswer answer = coach.ask(USER, "Siento dolor en la rodilla");

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
