package com.springbootwebsocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;

        // 模拟服务端每隔 3 秒向订阅者推送消息
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            String message = "服务端推送消息: " + System.currentTimeMillis();
            log.info("服务端推送消息: {}", message);
            messagingTemplate.convertAndSend("/topic/responses", message); // 推送到 /topic/updates
        }, 0, 3, TimeUnit.SECONDS);
    }

    // 处理前端通过 /tm/message 发送的消息
    @MessageMapping("tm/message")
    @SendTo("/topic/response")
    public String handleMessage(String message) {
        log.info("收到前端消息: {}", message);
        messagingTemplate.convertAndSend("/topic/responses", "服务端已收到消息: " + message);
        return "Broadcasted: " + message;
    }
}
