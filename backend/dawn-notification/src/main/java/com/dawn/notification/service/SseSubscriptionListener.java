package com.dawn.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseSubscriptionListener {

    void onSubscribed(SseEmitter emitter, String channel, String clientId);
}
