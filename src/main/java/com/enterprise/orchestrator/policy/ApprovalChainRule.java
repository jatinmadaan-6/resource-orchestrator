package com.enterprise.orchestrator.policy;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.ResourceType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** A request is pointless if nobody can approve it. */
@Component @Order(1)
public class ApprovalChainRule implements PolicyRule {
    @Override
    public Optional<String> check(AppUser requester, ResourceType type) {
        return requester.getManager() == null
                ? Optional.of("Requester has no manager assigned, so no approval chain exists")
                : Optional.empty();
    }
}
