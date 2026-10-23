package com.enterprise.orchestrator.domain;

import com.enterprise.orchestrator.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One physical unit (laptop serial) or one license seat. This is the "asset tracking" part. */
@Entity
@Table(name = "assets")
@Getter @NoArgsConstructor
public class Asset {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String assetTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser assignedTo;

    private Instant assignedAt;

    /** Optimistic lock: a second line of defence against double-assignment. */
    @Version
    private Long version;

    public Asset(String assetTag, ResourceType resourceType) {
        this.assetTag = assetTag; this.resourceType = resourceType;
    }

    public void assignTo(AppUser user) {
        if (status != AssetStatus.AVAILABLE) {
            throw new BusinessRuleException("Asset " + assetTag + " is not available");
        }
        this.status = AssetStatus.ASSIGNED;
        this.assignedTo = user;
        this.assignedAt = Instant.now();
    }

    public void release() {
        if (status != AssetStatus.ASSIGNED) {
            throw new BusinessRuleException("Asset " + assetTag + " is not currently assigned");
        }
        this.status = AssetStatus.AVAILABLE;
        this.assignedTo = null;
        this.assignedAt = null;
    }
}
