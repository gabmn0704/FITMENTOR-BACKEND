package com.fitmentor.domain.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingStatesTests {

    @Test
    void skipWalksWarmupSetRestSet() {
        TrainingState state = TrainingStates.warmup();

        state = state.skip();
        assertThat(state.phase()).isEqualTo(TrainingPhase.ACTIVE_SET);
        state = state.skip();
        assertThat(state.phase()).isEqualTo(TrainingPhase.REST);
        state = state.skip();
        assertThat(state.phase()).isEqualTo(TrainingPhase.ACTIVE_SET);
    }

    @Test
    void pauseRemembersActiveStateAndResumeRestoresIt() {
        TrainingState rest = TrainingStates.warmup().skip().skip();

        TrainingState paused = rest.pause();
        assertThat(paused.phase()).isEqualTo(TrainingPhase.PAUSED);
        assertThat(paused.resume()).isSameAs(rest);
    }

    @Test
    void controlsDoNotChangeStateWhilePaused() {
        TrainingState paused = TrainingStates.warmup().skip().pause();

        assertThat(paused.skip()).isSameAs(paused);
        assertThat(paused.pause()).isSameAs(paused);
    }

    @Test
    void resumeOutsidePauseKeepsState() {
        TrainingState active = TrainingStates.warmup().skip();

        assertThat(active.resume()).isSameAs(active);
    }

    @Test
    void finishedIsTerminal() {
        TrainingState finished = TrainingStates.warmup().finish();

        assertThat(finished.phase()).isEqualTo(TrainingPhase.FINISHED);
        assertThat(finished.skip()).isSameAs(finished);
        assertThat(finished.pause()).isSameAs(finished);
        assertThat(finished.resume()).isSameAs(finished);
        assertThat(finished.finish()).isSameAs(finished);
    }

    @Test
    void finishFromPausedEndsSession() {
        assertThat(TrainingStates.warmup().skip().pause().finish().phase()).isEqualTo(TrainingPhase.FINISHED);
    }
}
