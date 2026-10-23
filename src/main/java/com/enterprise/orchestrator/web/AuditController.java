package com.enterprise.orchestrator.web;

import com.enterprise.orchestrator.service.AuditService;
import com.enterprise.orchestrator.web.dto.AuditView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
@Tag(name = "Audit Trail")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    public List<AuditView> recent(@RequestParam(defaultValue = "50") int limit) { return audit.recent(limit); }

    @GetMapping("/requests/{id}")
    public List<AuditView> forRequest(@PathVariable Long id) { return audit.trailFor("ResourceRequest", id); }

    @GetMapping("/assets/{id}")
    public List<AuditView> forAsset(@PathVariable Long id) { return audit.trailFor("Asset", id); }
}
