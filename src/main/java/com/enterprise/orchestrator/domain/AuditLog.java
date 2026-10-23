package com.enterprise.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Append-only record of every business event. Never updated, never deleted. */
@Entity
@Table(name = "audit_logs", indexes = @Index(columnList = "entityType,entityId"))
@Getter @NoArgsConstructor
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String entityType;
    @Column(nullable = false) private Long entityId;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String actor;
    @Column(length = 1000)    private String details;
    @Column(nullable = false) private Instant timestamp = Instant.now();

    public AuditLog(String entityType, Long entityId, String action, String actor, String details) {
        this.entityType = entityType; this.entityId = entityId;
        this.action = action; this.actor = actor; this.details = details;
    }
}
