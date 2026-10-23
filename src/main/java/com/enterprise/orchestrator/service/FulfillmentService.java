package com.enterprise.orchestrator.service;

import com.enterprise.orchestrator.domain.*;
import com.enterprise.orchestrator.exception.BusinessRuleException;
import com.enterprise.orchestrator.exception.ResourceNotFoundException;
import com.enterprise.orchestrator.repository.AssetRepository;
import com.enterprise.orchestrator.repository.ResourceRequestRepository;
import com.enterprise.orchestrator.web.dto.RequestView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * IT half of the workflow: inventory check -> assignment -> asset tracking.
 * Request status, asset status and audit rows all change in ONE database transaction.
 */
@Service
public class FulfillmentService {

    private static final String ENTITY = "ResourceRequest";

    private final ResourceRequestRepository requests;
    private final AssetRepository assets;
    private final AuditService audit;

    public FulfillmentService(ResourceRequestRepository requests, AssetRepository assets, AuditService audit) {
        this.requests = requests; this.assets = assets; this.audit = audit;
    }

    /** IT admin clicks "assign": reserve a unit if one exists, otherwise backorder. */
    @Transactional
    public RequestView assign(Long requestId, String actor) {
        ResourceRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
        if (r.getStatus() != RequestStatus.APPROVED && r.getStatus() != RequestStatus.BACKORDERED) {
            throw new BusinessRuleException("Only APPROVED or BACKORDERED requests can be assigned (current: "
                    + r.getStatus() + ")");
        }
        tryAssign(r, actor);
        return RequestView.from(r);
    }

    /** Called whenever stock appears (new asset registered or one returned): serve the waiting list FIFO. */
    @Transactional
    public void fulfillBackorders(ResourceType type, String actor) {
        List<ResourceRequest> waiting =
                requests.findByResourceTypeAndStatusOrderByCreatedAtAsc(type, RequestStatus.BACKORDERED);
        for (ResourceRequest r : waiting) {
            if (!tryAssign(r, actor)) break; // out of stock again
        }
    }

    private boolean tryAssign(ResourceRequest r, String actor) {
        // Pessimistic lock: no other transaction can grab the same unit until we commit.
        List<Asset> stock = assets.findAvailableForUpdate(r.getResourceType(), AssetStatus.AVAILABLE, PageRequest.of(0, 1));

        if (stock.isEmpty()) {
            if (r.getStatus() == RequestStatus.APPROVED) {
                r.transitionTo(RequestStatus.BACKORDERED);
                audit.record(ENTITY, r.getId(), "BACKORDERED", actor, "No stock for " + r.getResourceType().getName());
            }
            return false;
        }

        Asset asset = stock.get(0);
        asset.assignTo(r.getRequester());
        r.setAssignedAsset(asset);
        r.transitionTo(RequestStatus.FULFILLED);
        audit.record(ENTITY, r.getId(), "ASSET_ASSIGNED", actor,
                "Assigned " + asset.getAssetTag() + " to " + r.getRequester().getUsername());
        return true;
    }
}
