package com.fitmentor.domain.pose.validation;

/** Outcome of the frame validation chain. A rejected frame is ignored silently by the socket. */
public record FrameVerdict(boolean accepted, String reason) {

    public static final FrameVerdict ACCEPTED = new FrameVerdict(true, "ok");

    public static FrameVerdict rejected(String reason) {
        return new FrameVerdict(false, reason);
    }
}
