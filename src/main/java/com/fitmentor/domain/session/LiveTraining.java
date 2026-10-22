package com.fitmentor.domain.session;

import com.fitmentor.domain.command.TrainingCommand;
import com.fitmentor.domain.command.TrainingCommandInvoker;
import com.fitmentor.domain.feedback.FeedbackEvent;
import com.fitmentor.domain.feedback.FeedbackObserver;
import com.fitmentor.domain.feedback.FeedbackSubject;
import com.fitmentor.domain.feedback.StateEvent;
import com.fitmentor.domain.pose.PoseAnalysis;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate of one live training session: state machine, command history and the subject
 * that notifies observers. Methods are synchronized because STOMP messages for the same
 * session may be handled on different threads.
 */
public final class LiveTraining {

    private final String sessionId;
    private final UUID owner;
    private final TrainingContext context = new TrainingContext(TrainingStates.warmup());
    private final TrainingCommandInvoker invoker = new TrainingCommandInvoker();
    private final FeedbackSubject subject = new FeedbackSubject();
    private String exercise;
    private Instant lastActivity;

    /** @param owner user that created the session, or null for an anonymous session. */
    public LiveTraining(String sessionId, UUID owner, Instant now) {
        this.sessionId = sessionId;
        this.owner = owner;
        this.lastActivity = now;
    }

    public String sessionId() {
        return sessionId;
    }

    public synchronized boolean ownedBy(UUID userId) {
        return owner == null || owner.equals(userId);
    }

    public synchronized Instant lastActivity() {
        return lastActivity;
    }

    public synchronized void touch(Instant now) {
        lastActivity = now;
    }

    public void subscribe(FeedbackObserver observer) {
        subject.subscribe(observer);
    }

    /** Executes a control command, records it for undo and publishes the new state. */
    public synchronized void apply(TrainingCommand command) {
        invoker.submit(command, context);
        publishState();
    }

    /** Reverts the last command. Returns false when there is nothing to undo. */
    public synchronized boolean undo() {
        boolean undone = invoker.undo(context);
        if (undone) {
            publishState();
        }
        return undone;
    }

    /**
     * Registers an analyzed frame: repetitions come from the AI service when it reports them
     * (local analysis reports none, so the count is left unchanged) and feedback is published.
     */
    public synchronized void recordAnalysis(PoseAnalysis analysis, String exerciseId) {
        if (exerciseId != null) {
            exercise = exerciseId;
        }
        if (analysis.reps() != null) {
            context.reportRepetitions(analysis.reps());
        }
        subject.publish(new FeedbackEvent(
            sessionId,
            String.join(" ", analysis.feedback()),
            analysis.score(),
            analysis.feedback(),
            analysis.phase(),
            context.repetitions(),
            context.state().phase().name(),
            exercise));
    }

    public synchronized TrainingPhase phase() {
        return context.state().phase();
    }

    private void publishState() {
        subject.publish(new StateEvent(sessionId, context.state().phase().name(), exercise));
    }
}
