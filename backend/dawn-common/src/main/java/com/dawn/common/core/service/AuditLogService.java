package com.dawn.common.core.service;

import com.dawn.common.core.model.AuditLog;
import com.dawn.common.core.repository.AuditLogRepository;
import com.dawn.common.core.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(String action, String entity, String entityId, String fromState, String toState, String metadata) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .actorId(SecurityUtils.getCurrentUserId())
                    .fromState(fromState)
                    .toState(toState)
                    .metadata(metadata != null && metadata.length() > 1024 ? metadata.substring(0, 1024) : metadata)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write audit log for {}:{}", entity, entityId, e);
        }
    }
}