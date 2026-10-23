package com.enterprise.orchestrator.web.dto;

import com.enterprise.orchestrator.domain.ResourceCategory;
import com.enterprise.orchestrator.domain.ResourceType;

import java.math.BigDecimal;

public record CatalogItemView(Long id, String name, ResourceCategory category, BigDecimal unitCost, int maxPerEmployee) {
    public static CatalogItemView from(ResourceType t) {
        return new CatalogItemView(t.getId(), t.getName(), t.getCategory(), t.getUnitCost(), t.getMaxPerEmployee());
    }
}
