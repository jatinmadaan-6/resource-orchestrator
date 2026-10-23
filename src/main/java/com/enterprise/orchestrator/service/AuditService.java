package com.enterprise.orchestrator.service;

import com.enterprise.orchestrator.domain.AuditLog;
import com.enterprise.orchestrator.repository.AuditLogRepository;
import com.enterprise.orchestrator.web.dto.AuditView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writes audit rows INSIDE the caller's transaction (Propagation.MANDATORY).
 * Consequence: a business change and its audit record commit together or roll back together,
 * so the trail can never disagree with reality.
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action, String actor, String details) {
        repo.save(new AuditLog(entityType, entityId, action, actor, details));
    }

    @Transactional(readOnly = true)
    public List<AuditView> trailFor(String entityType, Long entityId) {
        return repo.findByEntityTypeAndEntityIdOrderByTimestampAscIdAsc(entityType, entityId)
                .stream().map(AuditView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditView> recent(int limit) {
        return repo.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(limit, 500)))
                .stream().map(AuditView::from).toList();
    }
}
