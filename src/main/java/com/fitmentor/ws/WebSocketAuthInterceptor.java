package com.fitmentor.ws;

import com.fitmentor.domain.session.LiveTraining;
import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.security.JwtService;
import com.fitmentor.security.SessionChecker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards the inbound STOMP channel.
 *
 * <ul>
 *   <li>CONNECT: reads the optional {@code Authorization} header. A valid bearer token makes the
 *       session authenticated; no token keeps it anonymous; an invalid token rejects the connection.</li>
 *   <li>SEND: only {@code /app/pose} and {@code /app/command}. Clients cannot publish straight to
 *       {@code /topic/...}, so nobody can inject fake feedback into another session.</li>
 *   <li>SUBSCRIBE: only the exact per-session topics {@code /topic/feedback.<id>} and
 *       {@code /topic/state.<id>}. Wildcards are rejected (the simple broker would otherwise match
 *       {@code /topic/feedback.*} against every session), and a session owned by another user
 *       cannot be observed.</li>
 * </ul>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIX = "Bearer ";
    private static final Set<String> SEND_DESTINATIONS = Set.of("/app/pose", "/app/command");
    private static final Pattern TOPIC = Pattern.compile("^/topic/(?:feedback|state)\\.([A-Za-z0-9_-]{1,100})$");

    private final JwtService jwtService;
    private final SessionChecker sessions;
    private final ObjectProvider<LiveTrainingRegistry> registry;

    public WebSocketAuthInterceptor(JwtService jwtService, SessionChecker sessions,
                                    ObjectProvider<LiveTrainingRegistry> registry) {
        this.jwtService = jwtService;
        this.sessions = sessions;
        this.registry = registry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SEND -> checkSend(accessor.getDestination());
            case SUBSCRIBE -> checkSubscribe(accessor.getDestination(), TrainingSocketController.userId(accessor.getUser()));
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK... need no checks.
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        Optional<String> header = bearer(accessor.getNativeHeader("Authorization"));
        if (header.isEmpty()) {
            return;
        }
        AuthenticatedUser user = jwtService.verify(header.get())
            .filter(principal -> !sessions.isRevoked(principal))
            .orElseThrow(() -> new MessagingException("Invalid or expired token."));
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static void checkSend(String destination) {
        if (destination == null || !SEND_DESTINATIONS.contains(destination)) {
            throw new MessagingException("Destination not allowed.");
        }
    }

    private void checkSubscribe(String destination, UUID userId) {
        Matcher matcher = destination == null ? null : TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessagingException("Subscription not allowed.");
        }
        Optional<LiveTraining> live = registry.getObject().find(matcher.group(1));
        if (live.isPresent() && !live.get().ownedBy(userId)) {
            throw new MessagingException("Subscription not allowed.");
        }
    }

    private static Optional<String> bearer(List<String> values) {
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return Optional.empty();
        }
        String value = values.get(0).trim();
        if (!value.startsWith(PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(value.substring(PREFIX.length()).trim());
    }
}
