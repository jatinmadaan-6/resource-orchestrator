package com.enterprise.orchestrator.policy;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.ResourceType;
import org.springframework.stereotype.Service;

import java.util.List;

/** Runs every registered rule and collects ALL violations (not just the first). */
@Service
public class PolicyEngine {

    private final List<PolicyRule> rules; // Spring injects every PolicyRule bean, sorted by @Order

    public PolicyEngine(List<PolicyRule> rules) { this.rules = rules; }

    public List<String> validate(AppUser requester, ResourceType type) {
        return rules.stream()
                .map(rule -> rule.check(requester, type))
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
