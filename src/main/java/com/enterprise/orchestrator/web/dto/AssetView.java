package com.enterprise.orchestrator.web.dto;

import com.enterprise.orchestrator.domain.Asset;
import com.enterprise.orchestrator.domain.AssetStatus;

import java.time.Instant;

public record AssetView(Long id, String assetTag, String resourceType, AssetStatus status,
                        String assignedTo, Instant assignedAt) {
    public static AssetView from(Asset a) {
        return new AssetView(a.getId(), a.getAssetTag(), a.getResourceType().getName(), a.getStatus(),
                a.getAssignedTo() == null ? null : a.getAssignedTo().getUsername(), a.getAssignedAt());
    }
}
