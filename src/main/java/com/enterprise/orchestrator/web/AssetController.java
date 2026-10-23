package com.enterprise.orchestrator.web;

import com.enterprise.orchestrator.service.AssetService;
import com.enterprise.orchestrator.web.dto.AssetView;
import com.enterprise.orchestrator.web.dto.CatalogItemView;
import com.enterprise.orchestrator.web.dto.InventoryView;
import com.enterprise.orchestrator.web.dto.RegisterAssetRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Catalog & Inventory")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) { this.assetService = assetService; }

    @GetMapping("/catalog")
    public List<CatalogItemView> catalog() { return assetService.catalog(); }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    public List<InventoryView> inventory() { return assetService.inventory(); }

    @GetMapping("/assets")
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    public List<AssetView> assets() { return assetService.all(); }

    @PostMapping("/assets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    public AssetView register(@Valid @RequestBody RegisterAssetRequest body, Authentication auth) {
        return assetService.register(body.resourceTypeId(), body.assetTag(), auth.getName());
    }

    @PostMapping("/assets/{id}/return")
    @PreAuthorize("hasAnyRole('IT_ADMIN','ADMIN')")
    public AssetView returnAsset(@PathVariable Long id, Authentication auth) {
        return assetService.returnAsset(id, auth.getName());
    }
}
