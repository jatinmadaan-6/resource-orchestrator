package com.enterprise.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A catalog entry, e.g. "Dell XPS 15" or "IntelliJ Ultimate License". */
@Entity
@Table(name = "resource_types")
@Getter @Setter @NoArgsConstructor
public class ResourceType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ResourceCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    /** Entitlement: how many of these one employee may hold (assigned + in-flight requests). */
    @Column(nullable = false)
    private int maxPerEmployee;

    public ResourceType(String name, ResourceCategory category, BigDecimal unitCost, int maxPerEmployee) {
        this.name = name; this.category = category; this.unitCost = unitCost; this.maxPerEmployee = maxPerEmployee;
    }
}
