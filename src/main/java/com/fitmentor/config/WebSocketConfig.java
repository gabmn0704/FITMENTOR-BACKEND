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

/**
 * STOMP over WebSocket at {@code /ws}. Application destinations start with {@code /app} and
 * session replies use the {@code /topic} broker. The outbound channel is a decorated stack
 * (logging, then duplicate suppression) around the STOMP publisher.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
