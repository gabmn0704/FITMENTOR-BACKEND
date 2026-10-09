package com.fitmentor.domain.session;

public interface TrainingState {
    TrainingPhase phase();
    void next(TrainingContext context);
}
