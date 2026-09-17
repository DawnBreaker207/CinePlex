package com.dawn.common.core.service;

import com.dawn.common.core.model.AuditLog;
import com.dawn.common.core.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public static String clientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                return ip.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "";
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entity, String entityId, Long actorId,
                       String fromState, String toState, String metadata,
                       String status, String ipAddress, String oldValue, String newValue) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .actorId(actorId)
                    .fromState(fromState)
                    .toState(toState)
                    .metadata(metadata != null && metadata.length() > 4096 ? metadata.substring(0, 4096) : metadata)
                    .status(status)
                    .ipAddress(ipAddress)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write audit log for {}:{}", entity, entityId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String action, String entity, Long userId, String status,
                                 Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findAll(filter(action, entity, userId, status, from, to), pageable);
    }

    private static Specification<AuditLog> filter(String action, String entity, Long userId,
                                                  String status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null && !action.isBlank()) predicates.add(cb.equal(root.get("action"), action));
            if (entity != null && !entity.isBlank()) predicates.add(cb.equal(root.get("entity"), entity));
            if (userId != null) predicates.add(cb.equal(root.get("actorId"), userId));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}