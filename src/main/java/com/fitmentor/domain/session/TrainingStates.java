package com.fitmentor.domain.session;

import java.util.Objects;

/**
 * Concrete states of the State pattern. The transition table is:
 *
 * <pre>
 * WARMUP     --skip--&gt; ACTIVE_SET   --skip--&gt; REST    --skip--&gt; ACTIVE_SET
 * any active --pause--&gt; PAUSED --resume--&gt; the state that was active
 * any       --finish--&gt; FINISHED (terminal)
 * </pre>
 */
public final class TrainingStates {

    private TrainingStates() {
    }

    public static TrainingState warmup() {
        return Warmup.INSTANCE;
    }

    public static TrainingState finished() {
        return Finished.INSTANCE;
    }

    /** Initial state of every session. */
    static final class Warmup implements TrainingState {
        static final Warmup INSTANCE = new Warmup();

        public TrainingPhase phase() { return TrainingPhase.WARMUP; }
        public TrainingState pause() { return new Paused(this); }
        public TrainingState resume() { return this; }
        public TrainingState skip() { return ActiveSet.INSTANCE; }
        public TrainingState finish() { return Finished.INSTANCE; }
    }

    static final class ActiveSet implements TrainingState {
        static final ActiveSet INSTANCE = new ActiveSet();

        public TrainingPhase phase() { return TrainingPhase.ACTIVE_SET; }
        public TrainingState pause() { return new Paused(this); }
        public TrainingState resume() { return this; }
        public TrainingState skip() { return Rest.INSTANCE; }
        public TrainingState finish() { return Finished.INSTANCE; }
    }

    static final class Rest implements TrainingState {
        static final Rest INSTANCE = new Rest();

        public TrainingPhase phase() { return TrainingPhase.REST; }
        public TrainingState pause() { return new Paused(this); }
        public TrainingState resume() { return this; }
        public TrainingState skip() { return ActiveSet.INSTANCE; }
        public TrainingState finish() { return Finished.INSTANCE; }
    }

    /** Paused state: remembers the state to return to, so resume restores it exactly. */
    static final class Paused implements TrainingState {
        private final TrainingState resumeTo;

        Paused(TrainingState resumeTo) {
            this.resumeTo = Objects.requireNonNull(resumeTo);
        }

        TrainingState resumeTo() { return resumeTo; }

        public TrainingPhase phase() { return TrainingPhase.PAUSED; }
        public TrainingState pause() { return this; }
        public TrainingState resume() { return resumeTo; }
        public TrainingState skip() { return this; }
        public TrainingState finish() { return Finished.INSTANCE; }
    }

    /** Terminal state: every event is ignored. */
    static final class Finished implements TrainingState {
        static final Finished INSTANCE = new Finished();

        public TrainingPhase phase() { return TrainingPhase.FINISHED; }
        public TrainingState pause() { return this; }
        public TrainingState resume() { return this; }
        public TrainingState skip() { return this; }
        public TrainingState finish() { return this; }
    }
}
