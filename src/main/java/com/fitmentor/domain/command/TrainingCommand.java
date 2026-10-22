package com.fitmentor.domain.command;

import com.fitmentor.domain.session.TrainingContext;

/**
 * Command pattern: a user control ("pause", "skip", ...) encapsulated as an object.
 * Commands are executed and recorded by {@link TrainingCommandInvoker}, which keeps the
 * history needed for undo.
 */
public interface TrainingCommand {

    /** Wire name sent by the client, e.g. {@code pause}. */
    String name();

    void execute(TrainingContext context);
}
