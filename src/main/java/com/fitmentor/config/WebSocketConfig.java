package com.fitmentor.config;

import com.fitmentor.domain.feedback.FeedbackObserver;
import com.fitmentor.ws.DuplicateSuppressingFeedbackChannel;
import com.fitmentor.ws.FeedbackChannel;
import com.fitmentor.ws.LoggingFeedbackChannel;
import com.fitmentor.ws.StompFeedbackChannel;
import com.fitmentor.ws.StompFeedbackObserver;
import com.fitmentor.ws.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP over WebSocket at {@code /ws}. Application destinations start with {@code /app} and
 * session replies use the {@code /topic} broker. The outbound channel is a decorated stack
 * (logging, then duplicate suppression) around the STOMP publisher.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final int MAX_INBOUND_MESSAGE_BYTES = 8 * 1024;

    private final FitmentorProperties properties;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(FitmentorProperties properties, WebSocketAuthInterceptor authInterceptor) {
        this.properties = properties;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(properties.cors().allowedOrigins().toArray(String[]::new));
    }

    /**
     * Transport limits: a pose frame with 33 landmarks is about 3 KB, so inbound STOMP messages
     * are capped at 8 KB (also the default text buffer of Tomcat); slow consumers are dropped instead of buffering without bound.
     */
    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(MAX_INBOUND_MESSAGE_BYTES);
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setSendTimeLimit(10_000);
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Bean
    public FeedbackChannel feedbackChannel(SimpMessagingTemplate template) {
        return new DuplicateSuppressingFeedbackChannel(
            new LoggingFeedbackChannel(new StompFeedbackChannel(template)));
    }

    /** Observer that publishes every session event through the decorated channel. */
    @Bean
    public FeedbackObserver feedbackObserver(FeedbackChannel channel) {
        return new StompFeedbackObserver(channel);
    }
}
