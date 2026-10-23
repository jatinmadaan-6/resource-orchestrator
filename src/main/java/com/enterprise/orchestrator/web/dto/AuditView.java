package com.enterprise.orchestrator.web.dto;

import com.enterprise.orchestrator.domain.AuditLog;

import java.time.Instant;

public record AuditView(Long id, String entityType, Long entityId, String action,
                        String actor, String details, Instant timestamp) {
    public static AuditView from(AuditLog a) {
        return new AuditView(a.getId(), a.getEntityType(), a.getEntityId(), a.getAction(),
                a.getActor(), a.getDetails(), a.getTimestamp());
    }
}
