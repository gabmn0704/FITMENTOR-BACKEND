package com.fitmentor.domain.command;

import com.fitmentor.domain.session.TrainingContext;
import com.fitmentor.domain.session.TrainingPhase;
import com.fitmentor.domain.session.TrainingStates;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingCommandInvokerTests {

    @Test
    void commandsAreLookedUpByWireName() {
        assertThat(TrainingCommands.byName("pause")).map(TrainingCommand::name).contains("pause");
        assertThat(TrainingCommands.byName("skip")).isPresent();
        assertThat(TrainingCommands.byName("undo")).isEmpty();
        assertThat(TrainingCommands.byName(null)).isEmpty();
    }

    @Test
    void submitExecutesCommandOnContext() {
        TrainingContext context = new TrainingContext(TrainingStates.warmup());
        TrainingCommandInvoker invoker = new TrainingCommandInvoker();

        invoker.submit(TrainingCommands.byName("skip").orElseThrow(), context);

        assertThat(context.state().phase()).isEqualTo(TrainingPhase.ACTIVE_SET);
        assertThat(invoker.historySize()).isEqualTo(1);
    }

    @Test
    void undoRestoresStateAndRepetitionsFromMemento() {
        TrainingContext context = new TrainingContext(TrainingStates.warmup());
        TrainingCommandInvoker invoker = new TrainingCommandInvoker();
        invoker.submit(command("skip"), context);
        context.reportRepetitions(7);

        invoker.submit(command("pause"), context);
        assertThat(context.state().phase()).isEqualTo(TrainingPhase.PAUSED);

        assertThat(invoker.undo(context)).isTrue();
        assertThat(context.state().phase()).isEqualTo(TrainingPhase.ACTIVE_SET);
        assertThat(context.repetitions()).isEqualTo(7);
    }

    @Test
    void undoWithoutHistoryReturnsFalse() {
        TrainingContext context = new TrainingContext(TrainingStates.warmup());

        assertThat(new TrainingCommandInvoker().undo(context)).isFalse();
        assertThat(context.state().phase()).isEqualTo(TrainingPhase.WARMUP);
    }

    @Test
    void historyIsBounded() {
        TrainingContext context = new TrainingContext(TrainingStates.warmup());
        TrainingCommandInvoker invoker = new TrainingCommandInvoker();
        for (int i = 0; i < TrainingCommandInvoker.MAX_HISTORY + 5; i++) {
            invoker.submit(command("skip"), context);
        }

        assertThat(invoker.historySize()).isEqualTo(TrainingCommandInvoker.MAX_HISTORY);
    }

    @Test
    void reportedRepetitionsCannotBeNegative() {
        TrainingContext context = new TrainingContext(TrainingStates.warmup());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> context.reportRepetitions(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static TrainingCommand command(String name) {
        Optional<TrainingCommand> command = TrainingCommands.byName(name);
        return command.orElseThrow();
    }
}
