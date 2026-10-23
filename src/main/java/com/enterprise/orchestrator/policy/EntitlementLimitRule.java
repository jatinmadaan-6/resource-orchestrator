package com.enterprise.orchestrator.policy;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.AssetStatus;
import com.enterprise.orchestrator.domain.RequestStatus;
import com.enterprise.orchestrator.domain.ResourceType;
import com.enterprise.orchestrator.repository.AssetRepository;
import com.enterprise.orchestrator.repository.ResourceRequestRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Units already held + requests already in flight must stay below the per-employee limit. */
@Component @Order(3)
public class EntitlementLimitRule implements PolicyRule {

    private final AssetRepository assets;
    private final ResourceRequestRepository requests;

    public EntitlementLimitRule(AssetRepository assets, ResourceRequestRepository requests) {
        this.assets = assets; this.requests = requests;
    }

    @Override
    public Optional<String> check(AppUser requester, ResourceType type) {
        long held = assets.countByAssignedToAndResourceTypeAndStatus(requester, type, AssetStatus.ASSIGNED);
        long inFlight = requests.countByRequesterAndResourceTypeAndStatusIn(requester, type, RequestStatus.OPEN);
        return held + inFlight >= type.getMaxPerEmployee()
                ? Optional.of("Entitlement limit reached for '" + type.getName() + "' (max "
                        + type.getMaxPerEmployee() + ", holding " + held + ", in-flight " + inFlight + ")")
                : Optional.empty();
    }
}
