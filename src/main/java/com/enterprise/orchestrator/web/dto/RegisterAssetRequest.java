package com.enterprise.orchestrator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterAssetRequest(@NotNull Long resourceTypeId, @NotBlank String assetTag) { }
