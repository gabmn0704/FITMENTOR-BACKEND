package com.fitmentor.ws;

import com.fitmentor.domain.pose.validation.FrameValidationChain;
import com.fitmentor.security.AuthenticatedUser;
import com.fitmentor.security.JwtService;
import com.fitmentor.security.SessionChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTests {

    private final LiveTrainingRegistry registry = new LiveTrainingRegistry(
        event -> { }, new FrameValidationChain(Clock.systemUTC()), Clock.systemUTC());
    private final WebSocketAuthInterceptor interceptor;

    @SuppressWarnings("unchecked")
    WebSocketAuthInterceptorTests() {
        ObjectProvider<LiveTrainingRegistry> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(registry);
        interceptor = new WebSocketAuthInterceptor(mock(JwtService.class), mock(SessionChecker.class), provider);
    }

    @Test
    void clientsCanOnlySendToApplicationDestinations() {
        assertThatCode(() -> interceptor.preSend(message(StompCommand.SEND, "/app/pose", null), null))
            .doesNotThrowAnyException();
        assertThatCode(() -> interceptor.preSend(message(StompCommand.SEND, "/app/command", null), null))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/topic/feedback.s1", null), null))
            .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SEND, "/app/other", null), null))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    void wildcardAndForeignTopicSubscriptionsAreRejected() {
        assertThatCode(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/feedback.s-1", null), null))
            .doesNotThrowAnyException();
        for (String destination : List.of("/topic/feedback.*", "/topic/**", "/topic/feedback.{id}", "/queue/x")) {
            assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, destination, null), null))
                .isInstanceOf(MessagingException.class);
        }
    }

    @Test
    void onlyTheOwnerCanSubscribeToAnOwnedSession() {
        UUID owner = UUID.randomUUID();
        registry.getOrCreate("owned", owner);

        assertThatCode(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/state.owned", owner), null))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/state.owned", UUID.randomUUID()), null))
            .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.SUBSCRIBE, "/topic/feedback.owned", null), null))
            .isInstanceOf(MessagingException.class);
    }

    private static Message<byte[]> message(StompCommand command, String destination, UUID user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (user != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user, true), null, List.of()));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
