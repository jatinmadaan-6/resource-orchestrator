package com.enterprise.orchestrator.policy;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.ResourceType;

import java.util.Optional;

/**
 * Strategy pattern: each company rule is its own small class. Add a new rule by adding a new
 * @Component -- the PolicyEngine picks it up automatically (Open/Closed principle).
 */
public interface PolicyRule {
    /** @return a human-readable violation message, or empty if the request passes this rule. */
    Optional<String> check(AppUser requester, ResourceType type);
}
