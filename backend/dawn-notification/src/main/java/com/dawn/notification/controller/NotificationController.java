package com.dawn.notification.controller;

import com.dawn.common.core.dto.request.BookingNotificationEvent;
import com.dawn.common.core.helper.RedisKeyHelper;
import com.dawn.notification.service.EmailService;
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

    private final EmailService emailService;

    @GetMapping("/mail/test")
    public void sendEmail() {
        emailService.sendReservationEmail(BookingNotificationEvent
                .builder()
                .to("demo@gmail.com")
                .name("Dawnbreaker")
                .reservationId("ORD-D4DAAD940C10")
                .movieName("Overlord")
                .theaterName("Thanh Xuan")
                .showtimeSession("11/10/2025 12:00:00")
                .seats("A2,A3")
                .paymentTime("11/10/2025 12:00:00")
                .total("100000")
                .build()
        );
    }

    @GetMapping(value = "/subscribe/showtime/{showtimeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@PathVariable Long showtimeId, @RequestParam(defaultValue = "anonymous") String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store");
        headers.add(HttpHeaders.CONNECTION, "keep-alive");
        SseEmitter emitter = sseService.subscribe(RedisKeyHelper
                .showtimeChannel(showtimeId), clientId);
        return ResponseEntity
                .ok()
                .headers(headers)
                .body(emitter);
    }

}
