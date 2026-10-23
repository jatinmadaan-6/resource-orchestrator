package com.enterprise.orchestrator.web;

import com.enterprise.orchestrator.service.FulfillmentService;
import com.enterprise.orchestrator.service.RequestService;
import com.enterprise.orchestrator.web.dto.DecisionRequest;
import com.enterprise.orchestrator.web.dto.RequestView;
import com.enterprise.orchestrator.web.dto.SubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@Tag(name = "Resource Requests")
public class RequestController {

    private final RequestService requestService;
    private final FulfillmentService fulfillmentService;

    public RequestController(RequestService requestService, FulfillmentService fulfillmentService) {
        this.requestService = requestService; this.fulfillmentService = fulfillmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a request (runs policy validation automatically)")
    public RequestView submit(@Valid @RequestBody SubmitRequest body, Authentication auth) {
        return requestService.submit(auth.getName(), body.resourceTypeId(), body.justification());
    }

    @GetMapping("/mine")
    @Operation(summary = "My requests")
    public List<RequestView> mine(Authentication auth) { return requestService.mine(auth.getName()); }

    @GetMapping("/{id}")
    public RequestView get(@PathVariable Long id, Authentication auth) { return requestService.get(id, auth.getName()); }

    @PostMapping("/{id}/cancel")
    public RequestView cancel(@PathVariable Long id, Authentication auth) { return requestService.cancel(id, auth.getName()); }

    // ----- manager -----
    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(summary = "Manager inbox: pending requests from my direct reports")
    public List<RequestView> pending(Authentication auth) { return requestService.pendingApproval(auth.getName()); }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public RequestView approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest body, Authentication auth) {
        return requestService.approve(id, auth.getName(), body == null ? null : body.comment());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public RequestView reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionRequest body, Authentication auth) {
        return requestService.reject(id, auth.getName(), body == null ? null : body.comment());
    }

    // ----- IT -----
    @GetMapping("/fulfillment-queue")
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    @Operation(summary = "IT queue: approved + backordered requests, oldest first")
    public List<RequestView> queue() { return requestService.fulfillmentQueue(); }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    @Operation(summary = "Check inventory and assign an asset (or backorder)")
    public RequestView assign(@PathVariable Long id, Authentication auth) {
        return fulfillmentService.assign(id, auth.getName());
    }
}
