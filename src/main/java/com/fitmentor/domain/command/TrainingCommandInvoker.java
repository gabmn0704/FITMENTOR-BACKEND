package com.fitmentor.domain.command;

import com.fitmentor.domain.session.TrainingContext;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Invoker of the Command pattern. Before each command it stores a Memento of the context
 * (Memento pattern), so {@link #undo(TrainingContext)} can restore the previous state.
 * One invoker belongs to one live session, so it is not shared between threads.
 */
public class TrainingCommandInvoker {

    static final int MAX_HISTORY = 20;

    private final Deque<TrainingContext.Memento> history = new ArrayDeque<>();

    public void submit(TrainingCommand command, TrainingContext context) {
        history.push(context.save());
        if (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
        command.execute(context);
    }

    /** Reverts the last executed command. Returns false when there is nothing to undo. */
    public boolean undo(TrainingContext context) {
        if (history.isEmpty()) {
            return false;
        }
        context.restore(history.pop());
        return true;
    }

    public int historySize() {
        return history.size();
    }
}
