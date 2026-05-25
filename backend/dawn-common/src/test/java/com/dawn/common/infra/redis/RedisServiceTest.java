package com.dawn.common.infra.redis;

import com.dawn.common.infra.redis.service.RedisService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@TestPropertySource(properties = {
        "dawn.app.jwtSecret=test-secret-key-for-unit-test-only-32chars",
        "dawn.app.jwtExpirationsMs=86400000",
        "dawn.app.jwtRefreshExpirationsMs=604800000",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672"
})
@DisplayName("RedisService — Lua script atomicity")
class RedisServiceTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    RedisService redisService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String SEAT_KEY_1 = "seat:locked:1001";
    private static final String SEAT_KEY_2 = "seat:locked:1002";
    private static final String SEAT_KEY_3 = "seat:locked:1003";
    private static final String OWNER_A = "user:session:AAA";
    private static final String OWNER_B = "user:session:BBB";

    @AfterEach
    void cleanup() {
        stringRedisTemplate.delete(Arrays.asList(SEAT_KEY_1, SEAT_KEY_2, SEAT_KEY_3));
    }

    // ----------------------------------------------------------------
    // lockMulti — happy path
    // ----------------------------------------------------------------

    @Test
    @DisplayName("lockMulti: tất cả ghế FREE → lock thành công, trả về [1]")
    void lockMulti_allFree_shouldSucceed() {
        List<String> keys = Arrays.asList(SEAT_KEY_1, SEAT_KEY_2);

        List result = redisService.lockMulti(keys, OWNER_A, Duration.ofMinutes(15));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(1L);
        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_1)).isEqualTo(OWNER_A);
        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_2)).isEqualTo(OWNER_A);
    }

    @Test
    @DisplayName("lockMulti: TTL được set đúng (~15 phút)")
    void lockMulti_shouldSetCorrectTTL() {
        List<String> keys = Collections.singletonList(SEAT_KEY_1);

        redisService.lockMulti(keys, OWNER_A, Duration.ofMinutes(15));

        Long ttl = redisService.getExpired(SEAT_KEY_1);
        assertThat(ttl).isBetween(890L, 900L);
    }

    @Test
    @DisplayName("lockMulti: danh sách key rỗng → trả về [1] ngay")
    void lockMulti_emptyKeys_shouldReturnSuccess() {
        List result = redisService.lockMulti(Collections.emptyList(), OWNER_A, Duration.ofMinutes(15));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(1L);
    }

    // ----------------------------------------------------------------
    // lockMulti — conflict / partial fail
    // ----------------------------------------------------------------

    @Test
    @DisplayName("lockMulti: một ghế đã bị lock bởi owner khác → fail, trả về [0, key, currentOwner]")
    void lockMulti_oneKeyLockedByOther_shouldFail() {
        stringRedisTemplate.opsForValue().set(SEAT_KEY_2, OWNER_B, Duration.ofMinutes(15));

        List<String> keys = Arrays.asList(SEAT_KEY_1, SEAT_KEY_2);
        List result = redisService.lockMulti(keys, OWNER_A, Duration.ofMinutes(15));

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(0L);
        assertThat(result.get(1)).isEqualTo(SEAT_KEY_2);
        assertThat(result.get(2)).isEqualTo(OWNER_B);
    }

    @Test
    @DisplayName("lockMulti: partial fail → KHÔNG lock bất kỳ ghế nào (rollback atomically)")
    void lockMulti_partialFail_shouldNotLockAnyKey() {
        stringRedisTemplate.opsForValue().set(SEAT_KEY_3, OWNER_B, Duration.ofMinutes(15));

        List<String> keys = Arrays.asList(SEAT_KEY_1, SEAT_KEY_2, SEAT_KEY_3);
        redisService.lockMulti(keys, OWNER_A, Duration.ofMinutes(15));

        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_1)).isNull();
        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_2)).isNull();
    }

    @Test
    @DisplayName("lockMulti: cùng owner re-lock ghế đã lock của mình → thành công (idempotent)")
    void lockMulti_sameOwnerRelocks_shouldSucceed() {
        stringRedisTemplate.opsForValue().set(SEAT_KEY_1, OWNER_A, Duration.ofMinutes(15));

        List result = redisService.lockMulti(
                Collections.singletonList(SEAT_KEY_1), OWNER_A, Duration.ofMinutes(15));

        assertThat(result.get(0)).isEqualTo(1L);
    }

    // ----------------------------------------------------------------
    // releaseLock
    // ----------------------------------------------------------------

    @Test
    @DisplayName("releaseLock: đúng owner → unlock thành công, trả về true")
    void releaseLock_correctOwner_shouldReturnTrue() {
        stringRedisTemplate.opsForValue().set(SEAT_KEY_1, OWNER_A, Duration.ofMinutes(15));

        boolean released = redisService.releaseLock(SEAT_KEY_1, OWNER_A);

        assertThat(released).isTrue();
        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_1)).isNull();
    }

    @Test
    @DisplayName("releaseLock: sai owner → không unlock, trả về false")
    void releaseLock_wrongOwner_shouldReturnFalse() {
        stringRedisTemplate.opsForValue().set(SEAT_KEY_1, OWNER_A, Duration.ofMinutes(15));

        boolean released = redisService.releaseLock(SEAT_KEY_1, OWNER_B);

        assertThat(released).isFalse();
        assertThat(stringRedisTemplate.opsForValue().get(SEAT_KEY_1)).isEqualTo(OWNER_A);
    }

    @Test
    @DisplayName("releaseLock: key đã expire (không tồn tại) → trả về false")
    void releaseLock_expiredKey_shouldReturnFalse() {
        boolean released = redisService.releaseLock(SEAT_KEY_1, OWNER_A);

        assertThat(released).isFalse();
    }

    @Test
    @DisplayName("releaseLock: key null → trả về false, không throw")
    void releaseLock_nullKey_shouldReturnFalse() {
        boolean released = redisService.releaseLock(null, OWNER_A);
        assertThat(released).isFalse();
    }

    @Test
    @DisplayName("releaseLock: owner null → trả về false, không throw")
    void releaseLock_nullOwner_shouldReturnFalse() {
        boolean released = redisService.releaseLock(SEAT_KEY_1, null);
        assertThat(released).isFalse();
    }

    // ----------------------------------------------------------------
    // Race condition
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Race condition: 2 user lock cùng 1 ghế đồng thời → chỉ 1 winner")
    void lockMulti_concurrent_onlyOneWinner() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String owner = "user:session:RACER_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List result = redisService.lockMulti(
                            Collections.singletonList(SEAT_KEY_1),
                            owner,
                            Duration.ofMinutes(15));
                    if (Long.valueOf(1L).equals(result.get(0))) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Race condition: 5 user lock cùng 2 ghế → chỉ 1 winner duy nhất")
    void lockMulti_fiveConcurrent_onlyOneWinner() throws InterruptedException {
        int threadCount = 5;
        List<String> seatKeys = Arrays.asList(SEAT_KEY_1, SEAT_KEY_2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String owner = "user:session:USER_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List result = redisService.lockMulti(seatKeys, owner, Duration.ofMinutes(15));
                    if (Long.valueOf(1L).equals(result.get(0))) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
    }
}