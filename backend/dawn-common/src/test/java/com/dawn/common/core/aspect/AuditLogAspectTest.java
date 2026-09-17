package com.dawn.common.core.aspect;

import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.constant.LogConstant;
import com.dawn.common.core.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuditLogAspectTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogService service = mock(AuditLogService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final AuditLogAspect aspect =
            new AuditLogAspect(service, objectMapper, new AuditMessageBuilder(objectMapper), entityManager);

    @Test
    void record_evaluatesSpelParamsAndAuditContext() throws Throwable {
        AuditLog annotation = mock(AuditLog.class);
        when(annotation.action()).thenReturn("'TEST_ACTION'");
        when(annotation.entity()).thenReturn("'TEST_ENTITY'");
        when(annotation.entityId()).thenReturn("#reservationId");
        when(annotation.fromState()).thenReturn("#audit['fromState']");
        when(annotation.toState()).thenReturn("'NEW'");
        when(annotation.metadata()).thenReturn("'id=' + #reservationId");

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[]{"reservationId"});
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"RES-1"});
        when(pjp.proceed()).thenReturn("ok");

        AuditLogContext.set("fromState", "PENDING");
        Object result = aspect.record(pjp, annotation);

        assertThat(result).isEqualTo("ok");
        verify(service).record("TEST_ACTION", "TEST_ENTITY", "RES-1", null, "PENDING", "NEW",
                "id=RES-1", LogConstant.Status.SUCCESS, "", null, null);
        assertThat(AuditLogContext.get("fromState")).isNull();
    }

    @Test
    void record_methodThrows_writesFailedAuditAndClearsContext() throws Throwable {
        AuditLog annotation = mock(AuditLog.class);
        when(annotation.action()).thenReturn("'X'");
        when(annotation.entity()).thenReturn("'E'");

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[0]);
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        AuditLogContext.set("fromState", "PENDING");
        assertThatThrownBy(() -> aspect.record(pjp, annotation)).isInstanceOf(IllegalStateException.class);

        verify(service).record(eq("X"), eq("E"), isNull(), isNull(), isNull(), isNull(),
                contains("failed: boom"), eq(LogConstant.Status.FAILED), eq(""), isNull(), isNull());
        assertThat(AuditLogContext.get("fromState")).isNull();
    }

    @Test
    void record_blankExpressionsBecomeNull() throws Throwable {
        AuditLog annotation = mock(AuditLog.class);
        when(annotation.action()).thenReturn("'X'");
        when(annotation.entity()).thenReturn("'E'");
        when(annotation.entityId()).thenReturn("");
        when(annotation.fromState()).thenReturn("");
        when(annotation.toState()).thenReturn("'NEW'");
        when(annotation.metadata()).thenReturn("");

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getParameterNames()).thenReturn(new String[]{"id"});
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{"RES-1"});
        when(pjp.proceed()).thenReturn("ok");

        aspect.record(pjp, annotation);

        verify(service).record(eq("X"), eq("E"), eq("RES-1"), isNull(), isNull(), eq("NEW"),
                any(), eq(LogConstant.Status.SUCCESS), eq(""), isNull(), isNull());
    }
}