package com.dawn.common.core.aspect;

import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.constant.LogConstant;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final AuditMessageBuilder messageBuilder;
    private final EntityManager entityManager;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object record(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Long actorId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        String ip = getClientIp();
        String entityId = resolveParamId(pjp);
        String oldValue = captureOldValue(auditLog.entityClass(), entityId);

        try {
            Object result = pjp.proceed();
            if (entityId == null || entityId.isBlank()) {
                entityId = resolveResultId(result);
            }
            String newValue = messageBuilder.sanitize(serializeResult(result));
            String message = buildMessage(auditLog, pjp, username, entityId, oldValue, newValue, LogConstant.Status.SUCCESS, null);
            String finalEntityId = entityId;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        auditLogService.record(evaluate(auditLog.action(), pjp), evaluate(auditLog.entity(), pjp),
                                finalEntityId, actorId, evaluate(auditLog.fromState(), pjp), evaluate(auditLog.toState(), pjp),
                                message, LogConstant.Status.SUCCESS, ip, oldValue, newValue);
                    }
                });
            } else {
                auditLogService.record(evaluate(auditLog.action(), pjp), evaluate(auditLog.entity(), pjp),
                        finalEntityId, actorId, evaluate(auditLog.fromState(), pjp), evaluate(auditLog.toState(), pjp),
                        message, LogConstant.Status.SUCCESS, ip, oldValue, newValue);
            }
            return result;
        } catch (Throwable e) {
            String message = buildMessage(auditLog, pjp, username, entityId, oldValue, null, LogConstant.Status.FAILED, e.getMessage());
            try {
                auditLogService.record(evaluate(auditLog.action(), pjp), evaluate(auditLog.entity(), pjp),
                        entityId, actorId, evaluate(auditLog.fromState(), pjp), evaluate(auditLog.toState(), pjp),
                        message, LogConstant.Status.FAILED, ip, oldValue, null);
            } catch (Exception ex) {
                log.warn("Failed to write FAILED audit log", ex);
            }
            throw e;
        } finally {
            AuditLogContext.clear();
        }
    }

    private String buildMessage(AuditLog auditLog, ProceedingJoinPoint pjp, String username,
                                String entityId, String oldValue, String newValue, String status, String errorMsg) {
        String explicit = evaluate(auditLog.metadata(), pjp);
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        AuditMessageBuilder.MessageResult result = messageBuilder.build(
                username, evaluate(auditLog.action(), pjp), evaluate(auditLog.entity(), pjp),
                entityId, oldValue, newValue, status, errorMsg);
        return result.message();
    }

    private StandardEvaluationContext buildContext(ProceedingJoinPoint pjp) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (names != null) {
            for (int i = 0; i < names.length && i < args.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }
        ctx.setVariable("audit", AuditLogContext.snapshot());
        return ctx;
    }

    private String evaluate(String expression, ProceedingJoinPoint pjp) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            Object value = parser.parseExpression(expression).getValue(buildContext(pjp));
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            return expression;
        }
    }

    private String captureOldValue(Class<?> entityClass, String entityId) {
        if (entityClass == void.class || entityId == null || entityId.isBlank()) {
            return null;
        }
        Long id = parseId(entityId);
        if (id == null) {
            return null;
        }
        try {
            Object entity = entityManager.find(entityClass, id);
            if (entity == null) {
                return null;
            }
            return messageBuilder.sanitize(objectMapper.writeValueAsString(entity));
        } catch (Exception e) {
            log.warn("Failed to capture old value for {}: {}", entityClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private String serializeResult(Object result) {
        if (result == null) {
            return null;
        }
        Class<?> type = result.getClass();
        if (type == String.class || type.isPrimitive()
                || type == Boolean.class || type == Integer.class
                || type == Long.class || type == Double.class
                || type == Float.class || type == Short.class) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Failed to serialize result: {}", e.getMessage());
            return null;
        }
    }

    private String resolveParamId(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String name = paramNames[i].toLowerCase();
                if ((name.endsWith("id") || name.endsWith("ids")) && args[i] != null) {
                    return toIdString(args[i]);
                }
            }
        }
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            Optional<Long> id = tryExtractId(arg, "getId");
            if (id.isPresent()) {
                return id.get().toString();
            }
        }
        return null;
    }

    private String resolveResultId(Object result) {
        if (result != null) {
            Long id = tryExtractId(result, "getId")
                    .or(() -> tryExtractId(result, "id"))
                    .orElse(null);
            if (id != null) {
                return id.toString();
            }
        }
        Long userId = SecurityUtils.getCurrentUserId();
        return userId != null ? userId.toString() : "";
    }

    private Optional<Long> tryExtractId(Object obj, String methodName) {
        try {
            var m = obj.getClass().getMethod(methodName);
            if (m.getReturnType() != Void.TYPE && m.getParameterCount() == 0) {
                Object val = m.invoke(obj);
                if (val instanceof Number n) {
                    return Optional.of(n.longValue());
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private String toIdString(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return value.toString();
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value.split(",")[0].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                String first = ip.split(",")[0].trim();
                if (isValidIp(first)) {
                    return first;
                }
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isValidIp(String ip) {
        try {
            return InetAddress.getByName(ip).getHostAddress().equals(ip);
        } catch (Exception e) {
            return false;
        }
    }
}