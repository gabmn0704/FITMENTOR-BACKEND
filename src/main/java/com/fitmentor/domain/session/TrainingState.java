package com.fitmentor.domain.session;

/**
 * State pattern: the session state decides what each control event means.
 * States are immutable; every event returns the state the session moves to, so a
 * state can safely be kept in a memento for undo.
 */
public interface TrainingState {

    TrainingPhase phase();

    /** Pause the session, remembering the state to return to. */
    TrainingState pause();

    /** Resume after a pause; any other state keeps itself. */
    TrainingState resume();

    /** Move to the next block: warmup to set, set to rest, rest to set. */
    TrainingState skip();

    /** End the session. */
    TrainingState finish();
}
