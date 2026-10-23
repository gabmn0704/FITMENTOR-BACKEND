package com.fitmentor.ws;

import com.fitmentor.domain.pose.PoseFrame;
import com.fitmentor.security.AuthenticatedUser;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP endpoints: {@code SEND /app/pose} and {@code SEND /app/command}. Replies go only to
 * the per-session topics. Anonymous connections are allowed; their sessions have no owner.
 */
@Controller
public class TrainingSocketController {

    private final LiveTrainingService service;

    public TrainingSocketController(LiveTrainingService service) {
        this.service = service;
    }

    @MessageMapping("/pose")
    public void pose(@Payload PoseFrame frame, Principal principal) {
        service.handlePose(frame, userId(principal));
    }

    @MessageMapping("/command")
    public void command(@Payload CommandMessage message, Principal principal) {
        service.handleCommand(message, userId(principal));
    }

    /** Malformed payloads are ignored instead of closing the socket. */
    @MessageExceptionHandler
    public void ignored(Exception ignored) {
        // Intentionally silent: the client only receives messages for valid frames.
    }

    static UUID userId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication
            && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id();
        }
        return null;
    }
}
