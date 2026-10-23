package com.enterprise.orchestrator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRequest(@NotNull Long resourceTypeId,
                            @NotBlank @Size(max = 500) String justification) { }
