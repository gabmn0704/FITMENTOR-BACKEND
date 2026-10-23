package com.fitmentor.domain.pose.validation;

import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.PosePoint;

/** First link: rejects frames without landmarks or with a poor average confidence. */
public final class ConfidenceHandler extends FrameHandler {

    static final double MIN_AVERAGE_CONFIDENCE = 0.4;

    @Override
    protected FrameVerdict check(PoseFrame frame) {
        if (frame.points().isEmpty()) {
            return FrameVerdict.rejected("no_points");
        }
        double average = frame.points().stream().mapToDouble(PosePoint::confidence).average().orElse(0.0);
        return average >= MIN_AVERAGE_CONFIDENCE ? FrameVerdict.ACCEPTED : FrameVerdict.rejected("low_confidence");
    }
}
