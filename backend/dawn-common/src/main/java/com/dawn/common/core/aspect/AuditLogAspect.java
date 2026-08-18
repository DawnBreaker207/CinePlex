package com.dawn.common.core.aspect;

import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object record(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        try {
            Object result = pjp.proceed();
            recordAudit(auditLog, pjp);
            return result;
        } finally {
            AuditLogContext.clear();
        }
    }

    private void recordAudit(AuditLog auditLog, ProceedingJoinPoint pjp) {
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String[] names = signature.getParameterNames();
            Object[] args = pjp.getArgs();
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
            ctx.setVariable("audit", AuditLogContext.snapshot());
            auditLogService.record(
                    evaluate(auditLog.action(), ctx),
                    evaluate(auditLog.entity(), ctx),
                    evaluate(auditLog.entityId(), ctx),
                    evaluate(auditLog.fromState(), ctx),
                    evaluate(auditLog.toState(), ctx),
                    evaluate(auditLog.metadata(), ctx));
        } catch (Exception e) {
            log.warn("Failed to write audit log via @AuditLog", e);
        }
    }

    private String evaluate(String expression, StandardEvaluationContext ctx) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(ctx);
        return value != null ? String.valueOf(value) : null;
    }
}
