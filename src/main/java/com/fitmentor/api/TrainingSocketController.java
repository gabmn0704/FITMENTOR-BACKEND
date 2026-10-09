package com.fitmentor.api;

import com.fitmentor.domain.feedback.FeedbackEvent;
import com.fitmentor.domain.pose.PoseAnalysis;
import com.fitmentor.domain.pose.PoseAnalyzer;
import com.fitmentor.domain.pose.PoseFrame;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class TrainingSocketController {
    private final PoseAnalyzer poseAnalyzer;

    public TrainingSocketController(PoseAnalyzer poseAnalyzer) { this.poseAnalyzer = poseAnalyzer; }

    @MessageMapping("/pose")
    @SendTo("/topic/feedback")
    public FeedbackEvent analyze(PoseFrame frame) {
        PoseAnalysis result = poseAnalyzer.analyze(frame, "ai-service");
        return new FeedbackEvent("POSTURE", result.feedback(), result.score(), 0);
    }
}
