package com.dawn.notification.controller;

import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.notification.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final SseService sseService;

    @GetMapping(value = "/subscribe/showtime/{showtimeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@PathVariable Long showtimeId, @RequestParam String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.add(HttpHeaders.CONNECTION, "keep-alive");
        headers.add("X-Accel-Buffering", "no");

        SseEmitter emitter = sseService.subscribe(RedisKeyHelper
                .showtimeChannel(showtimeId), clientId);
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(emitter);
    }

}
