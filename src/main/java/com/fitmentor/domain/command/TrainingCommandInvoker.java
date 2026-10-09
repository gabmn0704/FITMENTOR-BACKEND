package com.fitmentor.domain.command;

public class TrainingCommandInvoker {
    public void submit(TrainingCommand command) { command.execute(); }
}
