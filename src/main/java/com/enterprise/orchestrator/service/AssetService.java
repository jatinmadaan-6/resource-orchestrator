package com.enterprise.orchestrator.service;

import com.enterprise.orchestrator.domain.*;
import com.enterprise.orchestrator.exception.BusinessRuleException;
import com.enterprise.orchestrator.exception.ResourceNotFoundException;
import com.enterprise.orchestrator.repository.AssetRepository;
import com.enterprise.orchestrator.repository.ResourceTypeRepository;
import com.enterprise.orchestrator.web.dto.AssetView;
import com.enterprise.orchestrator.web.dto.CatalogItemView;
import com.enterprise.orchestrator.web.dto.InventoryView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Inventory and asset-tracking operations. */
@Service
public class AssetService {

    private static final String ENTITY = "Asset";

    private final AssetRepository assets;
    private final ResourceTypeRepository types;
    private final FulfillmentService fulfillment;
    private final AuditService audit;

    public AssetService(AssetRepository assets, ResourceTypeRepository types,
                        FulfillmentService fulfillment, AuditService audit) {
        this.assets = assets; this.types = types; this.fulfillment = fulfillment; this.audit = audit;
    }

    @Transactional
    public AssetView register(Long typeId, String assetTag, String actor) {
        ResourceType type = types.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + typeId));
        if (assets.existsByAssetTag(assetTag)) {
            throw new BusinessRuleException("Asset tag already exists: " + assetTag);
        }
        Asset asset = assets.save(new Asset(assetTag, type));
        audit.record(ENTITY, asset.getId(), "REGISTERED", actor, assetTag + " (" + type.getName() + ")");
        assets.flush();
        fulfillment.fulfillBackorders(type, actor); // new stock may unblock waiting requests
        return AssetView.from(asset);
    }

    @Transactional
    public AssetView returnAsset(Long assetId, String actor) {
        Asset asset = assets.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        String previousHolder = asset.getAssignedTo() == null ? "?" : asset.getAssignedTo().getUsername();
        asset.release();
        audit.record(ENTITY, asset.getId(), "RETURNED", actor, "Returned by " + previousHolder);
        assets.flush();
        fulfillment.fulfillBackorders(asset.getResourceType(), actor);
        return AssetView.from(asset);
    }

    @Transactional(readOnly = true)
    public List<AssetView> all() {
        return assets.findAll().stream().map(AssetView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogItemView> catalog() {
        return types.findAll().stream().map(CatalogItemView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryView> inventory() {
        return types.findAll().stream().map(t -> new InventoryView(t.getId(), t.getName(), t.getCategory(),
                assets.countByResourceType(t),
                assets.countByResourceTypeAndStatus(t, AssetStatus.AVAILABLE),
                assets.countByResourceTypeAndStatus(t, AssetStatus.ASSIGNED))).toList();
    }
}
