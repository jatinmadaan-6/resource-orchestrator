package com.enterprise.orchestrator.policy;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** Expensive items must go through procurement, not this self-service flow. */
@Component @Order(2)
public class BudgetCeilingRule implements PolicyRule {

    private final BigDecimal ceiling;

    public BudgetCeilingRule(@Value("${app.policy.max-unit-cost:5000}") BigDecimal ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public Optional<String> check(AppUser requester, ResourceType type) {
        return type.getUnitCost().compareTo(ceiling) > 0
                ? Optional.of("'" + type.getName() + "' costs " + type.getUnitCost()
                        + ", above the self-service ceiling of " + ceiling + "; use the procurement process")
                : Optional.empty();
    }
}
