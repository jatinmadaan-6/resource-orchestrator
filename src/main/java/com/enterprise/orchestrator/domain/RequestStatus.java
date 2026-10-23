package com.enterprise.orchestrator.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a request. The legal transitions live here, in ONE place, so no service
 * can accidentally move a request into an impossible state.
 *
 * <pre>
 *  (submit) --policy fail--> REJECTED_BY_POLICY
 *      |
 *      v
 *  PENDING_APPROVAL --> REJECTED
 *      |            --> CANCELLED
 *      v
 *   APPROVED --> FULFILLED
 *      |     --> BACKORDERED --> FULFILLED
 *      |     --> CANCELLED       (also from BACKORDERED)
 * </pre>
 */
public enum RequestStatus {
    PENDING_APPROVAL, APPROVED, BACKORDERED, FULFILLED, REJECTED, REJECTED_BY_POLICY, CANCELLED;

    /** Statuses that still "count" against an employee's entitlement. */
    public static final Set<RequestStatus> OPEN = EnumSet.of(PENDING_APPROVAL, APPROVED, BACKORDERED);

    public boolean canTransitionTo(RequestStatus next) {
        return switch (this) {
            case PENDING_APPROVAL -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == FULFILLED || next == BACKORDERED || next == CANCELLED;
            case BACKORDERED -> next == FULFILLED || next == CANCELLED;
            default -> false; // terminal states
        };
    }
}
