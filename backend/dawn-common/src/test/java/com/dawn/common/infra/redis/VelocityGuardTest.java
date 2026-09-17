package com.dawn.common.infra.redis;

import com.dawn.common.infra.redis.service.RedisService;
import com.dawn.common.infra.redis.service.VelocityGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VelocityGuard")
class VelocityGuardTest {

    @Mock
    RedisService redisService;

    @InjectMocks
    VelocityGuard guard;

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquire {

        @Test
        @DisplayName("under limit → allow and set window once")
        void underLimit_allow() {
            when(redisService.increment("k", 1L)).thenReturn(1L);

            assertThat(guard.tryAcquire("k", 5, Duration.ofMinutes(10))).isTrue();
            verify(redisService).expire("k", Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("over limit → deny")
        void overLimit_deny() {
            when(redisService.increment("k", 1L)).thenReturn(6L);

            assertThat(guard.tryAcquire("k", 5, Duration.ofMinutes(10))).isFalse();
        }

        @Test
        @DisplayName("Redis down → fail-open allow")
        void redisDown_failOpen() {
            when(redisService.increment(anyString(), any())).thenThrow(new RuntimeException("down"));

            assertThat(guard.tryAcquire("k", 5, Duration.ofMinutes(10))).isTrue();
        }
    }
}
