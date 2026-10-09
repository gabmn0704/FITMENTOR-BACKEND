package com.fitmentor.domain.session;

public final class TrainingStates {
    private TrainingStates() {}

    public static final class Warmup implements TrainingState {
        public TrainingPhase phase() { return TrainingPhase.WARMUP; }
        public void next(TrainingContext context) { context.transitionTo(new ActiveSet()); }
    }

    public static final class ActiveSet implements TrainingState {
        public TrainingPhase phase() { return TrainingPhase.ACTIVE_SET; }
        public void next(TrainingContext context) { context.transitionTo(new Rest()); }
    }

    public static final class Rest implements TrainingState {
        public TrainingPhase phase() { return TrainingPhase.REST; }
        public void next(TrainingContext context) { context.transitionTo(new ActiveSet()); }
    }

    public static final class Finished implements TrainingState {
        public TrainingPhase phase() { return TrainingPhase.FINISHED; }
        public void next(TrainingContext context) { }
    }
}
