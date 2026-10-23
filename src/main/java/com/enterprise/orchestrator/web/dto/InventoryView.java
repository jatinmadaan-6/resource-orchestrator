package com.enterprise.orchestrator.web.dto;

import com.enterprise.orchestrator.domain.ResourceCategory;

public record InventoryView(Long resourceTypeId, String name, ResourceCategory category,
                            long total, long available, long assigned) { }
