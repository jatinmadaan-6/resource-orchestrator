package com.enterprise.orchestrator.web.dto;

import com.enterprise.orchestrator.domain.RequestStatus;
import com.enterprise.orchestrator.domain.ResourceRequest;

import java.time.Instant;

public record RequestView(Long id, String requester, String resourceType, String justification,
                          RequestStatus status, String decidedBy, String decisionComment,
                          String assignedAssetTag, Instant createdAt, Instant updatedAt) {

    /** Must be called inside a transaction (touches lazy associations). */
    public static RequestView from(ResourceRequest r) {
        return new RequestView(r.getId(), r.getRequester().getUsername(), r.getResourceType().getName(),
                r.getJustification(), r.getStatus(),
                r.getDecidedBy() == null ? null : r.getDecidedBy().getUsername(),
                r.getDecisionComment(),
                r.getAssignedAsset() == null ? null : r.getAssignedAsset().getAssetTag(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
