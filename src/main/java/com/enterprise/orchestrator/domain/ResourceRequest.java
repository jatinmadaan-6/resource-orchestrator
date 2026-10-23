package com.enterprise.orchestrator.domain;

import com.enterprise.orchestrator.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "resource_requests")
@Getter @Setter @NoArgsConstructor
public class ResourceRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private AppUser requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ResourceType resourceType;

    @Column(nullable = false, length = 500)
    private String justification;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RequestStatus status;

    /** Manager/admin who approved or rejected. */
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser decidedBy;

    @Column(length = 500)
    private String decisionComment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Asset assignedAsset;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    public ResourceRequest(AppUser requester, ResourceType type, String justification, RequestStatus initial) {
        this.requester = requester; this.resourceType = type;
        this.justification = justification; this.status = initial;
    }

    /** The only way status changes after creation: guarded by the state machine. */
    public void transitionTo(RequestStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleException("Illegal transition " + status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }
}
