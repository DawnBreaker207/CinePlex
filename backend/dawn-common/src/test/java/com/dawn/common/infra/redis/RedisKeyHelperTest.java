package com.dawn.common.infra.redis;

import com.dawn.common.core.helper.RedisKeyHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisKeyHelper - key format")
public class RedisKeyHelperTest {

    @Test
    @DisplayName("reservationHoldKey: đúng format")
    void reservationHoldKey_shouldReturnCorrectFormat() {
        String key = RedisKeyHelper.reservationHoldKey("abc-123");
        assertThat(key).isEqualTo("reservation:data:abc-123");
    }

    @Test
    @DisplayName("seatLockKey: đúng format")
    void seatLockKey_shouldReturnCorrectFormat() {
        String key = RedisKeyHelper.seatLockKey(42L);
        assertThat(key).isEqualTo("seat:locked:42");
    }

    @Test
    @DisplayName("showtimeChannel: đúng format")
    void showtimeChannel_shouldReturnCorrectFormat() {
        String key = RedisKeyHelper.showtimeChannel(7L);
        assertThat(key).isEqualTo("channel:showtime:7");
    }

    @Test
    @DisplayName("seatLockKey: các seat khác nhau cho key khác nhau")
    void seatLockKey_differentIds_shouldProduceDifferentKeys() {
        String key1 = RedisKeyHelper.seatLockKey(1L);
        String key2 = RedisKeyHelper.seatLockKey(2L);
        assertThat(key1).isNotEqualTo(key2);
    }

}
