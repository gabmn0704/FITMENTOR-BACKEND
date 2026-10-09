package com.fitmentor.domain.session;

public class TrainingContext {
    private TrainingState state;
    private int repetitions;

    public TrainingContext(TrainingState initialState) {
        this.state = initialState;
    }

    public void transitionTo(TrainingState nextState) {
        state = nextState;
    }

    public void countRepetition() {
        repetitions++;
    }

    public TrainingState state() { return state; }
    public int repetitions() { return repetitions; }
}
