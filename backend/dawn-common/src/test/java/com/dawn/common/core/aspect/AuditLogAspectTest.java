package com.dawn.common.core.aspect;

import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuditLogAspectTest {

    @Test
    void record_evaluatesSpelParamsAndAuditContext() throws Throwable {
        AuditLogService service = mock(AuditLogService.class);
        AuditLogAspect aspect = new AuditLogAspect(service);
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
        verify(service).record("TEST_ACTION", "TEST_ENTITY", "RES-1", "PENDING", "NEW", "id=RES-1");
        assertThat(AuditLogContext.get("fromState")).isNull();
    }

    @Test
    void record_methodThrows_skipsAuditAndClearsContext() throws Throwable {
        AuditLogService service = mock(AuditLogService.class);
        AuditLogAspect aspect = new AuditLogAspect(service);
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

        verify(service, never()).record(any(), any(), any(), any(), any(), any());
        assertThat(AuditLogContext.get("fromState")).isNull();
    }

    @Test
    void record_blankExpressionsBecomeNull() throws Throwable {
        AuditLogService service = mock(AuditLogService.class);
        AuditLogAspect aspect = new AuditLogAspect(service);
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

        verify(service).record("X", "E", null, null, "NEW", null);
    }
}
