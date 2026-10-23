package com.enterprise.orchestrator.service;

import com.enterprise.orchestrator.domain.*;
import com.enterprise.orchestrator.exception.ForbiddenOperationException;
import com.enterprise.orchestrator.exception.ResourceNotFoundException;
import com.enterprise.orchestrator.policy.PolicyEngine;
import com.enterprise.orchestrator.repository.ResourceRequestRepository;
import com.enterprise.orchestrator.repository.ResourceTypeRepository;
import com.enterprise.orchestrator.web.dto.RequestView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Employee- and manager-facing half of the workflow: submit -> policy check -> approve/reject/cancel. */
@Service
public class RequestService {

    private static final String ENTITY = "ResourceRequest";

    private final ResourceRequestRepository requests;
    private final ResourceTypeRepository types;
    private final PolicyEngine policyEngine;
    private final AuditService audit;
    private final UserLookup userLookup;

    public RequestService(ResourceRequestRepository requests, ResourceTypeRepository types,
                          PolicyEngine policyEngine, AuditService audit, UserLookup userLookup) {
        this.requests = requests; this.types = types; this.policyEngine = policyEngine;
        this.audit = audit; this.userLookup = userLookup;
    }

    // ---------- Step 1 + 2: submission and automatic policy validation ----------
    @Transactional
    public RequestView submit(String username, Long resourceTypeId, String justification) {
        AppUser requester = userLookup.byUsername(username);
        ResourceType type = types.findById(resourceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + resourceTypeId));

        List<String> violations = policyEngine.validate(requester, type);
        boolean passed = violations.isEmpty();

        ResourceRequest request = new ResourceRequest(requester, type, justification,
                passed ? RequestStatus.PENDING_APPROVAL : RequestStatus.REJECTED_BY_POLICY);
        if (!passed) request.setDecisionComment(String.join("; ", violations));
        requests.save(request);

        audit.record(ENTITY, request.getId(), "SUBMITTED", username, "Requested " + type.getName());
        audit.record(ENTITY, request.getId(), passed ? "POLICY_PASSED" : "POLICY_REJECTED", "SYSTEM",
                passed ? "All policy rules satisfied" : request.getDecisionComment());
        return RequestView.from(request);
    }

    // ---------- Step 3: manager approval ----------
    @Transactional
    public RequestView approve(Long id, String approverName, String comment) {
        return decide(id, approverName, comment, RequestStatus.APPROVED, "APPROVED");
    }

    @Transactional
    public RequestView reject(Long id, String approverName, String comment) {
        return decide(id, approverName, comment, RequestStatus.REJECTED, "REJECTED");
    }

    private RequestView decide(Long id, String approverName, String comment, RequestStatus target, String action) {
        AppUser approver = userLookup.byUsername(approverName);
        ResourceRequest r = load(id);

        if (Objects.equals(r.getRequester().getId(), approver.getId())) {
            throw new ForbiddenOperationException("You cannot decide your own request");
        }
        boolean isDirectManager = r.getRequester().getManager() != null
                && Objects.equals(r.getRequester().getManager().getId(), approver.getId());
        if (!isDirectManager && approver.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Only the requester's manager (or an admin) can decide this request");
        }

        r.transitionTo(target);           // state machine rejects illegal moves
        r.setDecidedBy(approver);
        r.setDecisionComment(comment);
        audit.record(ENTITY, r.getId(), action, approverName, comment);
        return RequestView.from(r);
    }

    // ---------- Cancel (requester withdraws) ----------
    @Transactional
    public RequestView cancel(Long id, String username) {
        AppUser user = userLookup.byUsername(username);
        ResourceRequest r = load(id);
        if (!Objects.equals(r.getRequester().getId(), user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Only the requester can cancel a request");
        }
        r.transitionTo(RequestStatus.CANCELLED);
        audit.record(ENTITY, r.getId(), "CANCELLED", username, null);
        return RequestView.from(r);
    }

    // ---------- Queries ----------
    @Transactional(readOnly = true)
    public List<RequestView> mine(String username) {
        return requests.findByRequesterOrderByCreatedAtDesc(userLookup.byUsername(username))
                .stream().map(RequestView::from).toList();
    }

    /** Manager inbox. Admins see everything pending. */
    @Transactional(readOnly = true)
    public List<RequestView> pendingApproval(String username) {
        AppUser user = userLookup.byUsername(username);
        List<ResourceRequest> list = user.getRole() == Role.ADMIN
                ? requests.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING_APPROVAL)
                : requests.findForManager(user, RequestStatus.PENDING_APPROVAL);
        return list.stream().map(RequestView::from).toList();
    }

    /** IT queue: approved requests plus backorders, oldest first. */
    @Transactional(readOnly = true)
    public List<RequestView> fulfillmentQueue() {
        return requests.findByStatusInOrderByCreatedAtAsc(List.of(RequestStatus.APPROVED, RequestStatus.BACKORDERED))
                .stream().map(RequestView::from).toList();
    }

    @Transactional(readOnly = true)
    public RequestView get(Long id, String username) {
        AppUser user = userLookup.byUsername(username);
        ResourceRequest r = load(id);
        boolean owner = Objects.equals(r.getRequester().getId(), user.getId());
        boolean manager = r.getRequester().getManager() != null
                && Objects.equals(r.getRequester().getManager().getId(), user.getId());
        boolean privileged = user.getRole() == Role.ADMIN || user.getRole() == Role.IT_ADMIN;
        if (!owner && !manager && !privileged) {
            throw new ForbiddenOperationException("You may not view this request");
        }
        return RequestView.from(r);
    }

    private ResourceRequest load(Long id) {
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Request not found: " + id));
    }
}
