package com.springbootwebsocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.socket.config.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
@CrossOrigin
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    Set<String> userIdSet = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(50)
                .maxPoolSize(100)
                .keepAliveSeconds(60);
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("收到连接请求");
                    if (accessor.containsNativeHeader("uid")) {
                        String uid = accessor.getNativeHeader("uid").get(0);
                        userIdSet.add(uid);
                        log.info("{}已连接", uid);
                        Principal principal = () -> uid;
                        accessor.setUser(principal);
                    }
                } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    if (accessor.getUser() != null) userIdSet.remove(accessor.getUser().getName());
                    log.info("用户 {} 已断开连接", accessor.getUser().getName());
                }
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/stomp/tm")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/tm");
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public Set<String> userIdSet() {
        return userIdSet;
    }
}
