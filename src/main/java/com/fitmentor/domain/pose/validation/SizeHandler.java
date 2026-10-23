package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;

/** Last link: rejects frames with impossible dimensions or an absurd number of landmarks. */
public final class SizeHandler extends FrameHandler {

    static final int MAX_WIDTH = 7680;
    static final int MAX_HEIGHT = 4320;
    static final int MAX_POINTS = 64;

    @Override
    protected FrameVerdict check(PoseFrame frame) {
        boolean validSize = frame.frameWidth() > 0 && frame.frameWidth() <= MAX_WIDTH
            && frame.frameHeight() > 0 && frame.frameHeight() <= MAX_HEIGHT;
        if (!validSize) {
            return FrameVerdict.rejected("invalid_size");
        }
        return frame.points().size() <= MAX_POINTS ? FrameVerdict.ACCEPTED : FrameVerdict.rejected("too_many_points");
    }
}
