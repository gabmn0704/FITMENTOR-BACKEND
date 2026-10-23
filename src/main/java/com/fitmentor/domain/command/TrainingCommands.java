package com.fitmentor.domain.command;

import com.fitmentor.domain.session.TrainingContext;

import java.util.Map;
import java.util.Optional;

/**
 * Concrete commands of the Command pattern and their lookup by wire name.
 * {@code undo} is not a command here: it is handled by the invoker through the memento history.
 */
public final class TrainingCommands {

    private static final Map<String, TrainingCommand> BY_NAME = Map.of(
        "pause", new Pause(),
        "resume", new Resume(),
        "skip", new Skip(),
        "finish", new Finish()
    );

    private TrainingCommands() {
    }

    public static Optional<TrainingCommand> byName(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(BY_NAME.get(name));
    }

    static final class Pause implements TrainingCommand {
        public String name() { return "pause"; }
        public void execute(TrainingContext context) { context.transitionTo(context.state().pause()); }
    }

    static final class Resume implements TrainingCommand {
        public String name() { return "resume"; }
        public void execute(TrainingContext context) { context.transitionTo(context.state().resume()); }
    }

    static final class Skip implements TrainingCommand {
        public String name() { return "skip"; }
        public void execute(TrainingContext context) { context.transitionTo(context.state().skip()); }
    }

    static final class Finish implements TrainingCommand {
        public String name() { return "finish"; }
        public void execute(TrainingContext context) { context.transitionTo(context.state().finish()); }
    }
}
