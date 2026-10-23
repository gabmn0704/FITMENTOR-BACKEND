package com.fitmentor.ws;

import com.fitmentor.ai.AiServicePoseStrategy;
import com.fitmentor.domain.command.TrainingCommands;
import com.fitmentor.domain.pose.PoseAnalysis;
import com.fitmentor.domain.pose.PoseAnalyzer;
import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.domain.pose.validation.FrameValidationChain;
import com.fitmentor.domain.pose.validation.FrameVerdict;
import com.fitmentor.domain.session.LiveTraining;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use cases of the real-time channel: analyze a pose frame and apply a control command.
 * Invalid, rate-limited or foreign-session messages are dropped silently.
 */
@Service
public class LiveTrainingService {

    static final int MAX_EXERCISE_LENGTH = 60;

    private static final Logger logger = LoggerFactory.getLogger(LiveTrainingService.class);

    private final FrameValidationChain frameChain;
    private final PoseAnalyzer analyzer;
    private final LiveTrainingRegistry registry;

    public LiveTrainingService(FrameValidationChain frameChain, PoseAnalyzer analyzer, LiveTrainingRegistry registry) {
        this.frameChain = frameChain;
        this.analyzer = analyzer;
        this.registry = registry;
    }

    public void handlePose(PoseFrame frame, UUID userId) {
        if (frame == null || !LiveTrainingRegistry.isValidSessionId(frame.sessionId())) {
            return;
        }
        if (frame.exercise() == null || frame.exercise().length() > MAX_EXERCISE_LENGTH) {
            return;
        }
        FrameVerdict verdict = frameChain.validate(frame);
        if (!verdict.accepted()) {
            logger.debug("Dropped frame: {}", verdict.reason());
            return;
        }
        LiveTraining live = registry.getOrCreate(frame.sessionId(), userId).orElse(null);
        if (live == null || !live.ownedBy(userId)) {
            return;
        }
        PoseAnalysis analysis = analyzer.analyze(frame, AiServicePoseStrategy.ENGINE);
        live.recordAnalysis(analysis, frame.exercise());
    }

    public void handleCommand(CommandMessage message, UUID userId) {
        if (message == null || !LiveTrainingRegistry.isValidSessionId(message.sessionId()) || isBlank(message.name())) {
            return;
        }
        LiveTraining live = registry.getOrCreate(message.sessionId(), userId).orElse(null);
        if (live == null || !live.ownedBy(userId)) {
            return;
        }
        if ("undo".equals(message.name())) {
            live.undo();
            return;
        }
        TrainingCommands.byName(message.name()).ifPresent(command -> live.apply(command));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank() || value.length() > 100;
    }
}
