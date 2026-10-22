package com.fitmentor.ws;

import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.security.JwtService;
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

/**
 * On STOMP CONNECT, reads the optional {@code Authorization} header. A valid bearer token
 * makes the session authenticated; no token keeps it anonymous; an invalid token rejects the
 * connection.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }
        Optional<String> header = bearer(accessor.getNativeHeader("Authorization"));
        if (header.isEmpty()) {
            return message;
        }
        AuthenticatedUser user = jwtService.verify(header.get())
            .orElseThrow(() -> new MessagingException("Invalid or expired token."));
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        return message;
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
