package com.inuteamflow.server.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final WebSocketHandshakeLoggingInterceptor webSocketHandshakeLoggingInterceptor;
    private final WebSocketTransportLoggingDecoratorFactory webSocketTransportLoggingDecoratorFactory;
    private final StompLoggingErrorHandler stompLoggingErrorHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(stompLoggingErrorHandler);
        registry.addEndpoint("/ws-chat")
                .addInterceptors(webSocketHandshakeLoggingInterceptor)
                .setAllowedOriginPatterns("*"); // 추후에 실제 프론트 도메인으로 제한 필요
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub") // 서버 → 클라이언트 구독 경로
                .setHeartbeatValue(new long[] {10000, 10000})
                .setTaskScheduler(heartBeatTaskScheduler());
        registry.setApplicationDestinationPrefixes("/pub"); // 클라이언트 → 서버 발행 경로
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(webSocketTransportLoggingDecoratorFactory);
    }

    @Bean
    public TaskScheduler heartBeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
