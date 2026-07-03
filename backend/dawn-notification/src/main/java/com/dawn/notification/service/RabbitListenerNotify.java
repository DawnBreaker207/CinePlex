package com.dawn.notification.service;

import com.dawn.common.core.constant.Constants;
import com.dawn.common.core.constant.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitListenerNotify {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConstants.QUEUE_DASHBOARD)
    public void handleUpdateDashboard() {
        Map<String, String> payload = Collections.singletonMap(Constants.SSE_FIELD_ACTION, "REFRESH");
        messagingTemplate.convertAndSend("/topic/dashboard/update", payload);
    }
}
