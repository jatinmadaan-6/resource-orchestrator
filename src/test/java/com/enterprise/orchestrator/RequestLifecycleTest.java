package com.enterprise.orchestrator;

import com.enterprise.orchestrator.domain.*;
import com.enterprise.orchestrator.exception.BusinessRuleException;
import com.enterprise.orchestrator.exception.ForbiddenOperationException;
import com.enterprise.orchestrator.repository.ResourceTypeRepository;
import com.enterprise.orchestrator.service.*;
import com.enterprise.orchestrator.web.dto.AuditView;
import com.enterprise.orchestrator.web.dto.RequestView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end workflow tests against the real services and an in-memory H2 DB (rolled back after each test). */
@SpringBootTest
@Transactional
class RequestLifecycleTest {

    @Autowired RequestService requests;
    @Autowired FulfillmentService fulfillment;
    @Autowired AssetService assetService;
    @Autowired AuditService audit;
    @Autowired ResourceTypeRepository types;

    private Long typeId(String namePart) {
        return types.findAll().stream().filter(t -> t.getName().contains(namePart)).findFirst().orElseThrow().getId();
    }

    @Test
    void happyPath_submit_approve_assign() {
        RequestView r = requests.submit("bob", typeId("Laptop"), "New joiner");
        assertEquals(RequestStatus.PENDING_APPROVAL, r.status());

        r = requests.approve(r.id(), "alice", "OK");
        assertEquals(RequestStatus.APPROVED, r.status());

        r = fulfillment.assign(r.id(), "ian");
        assertEquals(RequestStatus.FULFILLED, r.status());
        assertNotNull(r.assignedAssetTag());

        List<String> actions = audit.trailFor("ResourceRequest", r.id()).stream().map(AuditView::action).toList();
        assertEquals(List.of("SUBMITTED", "POLICY_PASSED", "APPROVED", "ASSET_ASSIGNED"), actions);
    }

    @Test
    void policy_blocksSecondLaptopWhileFirstIsInFlight() {
        requests.submit("bob", typeId("Laptop"), "first");
        RequestView second = requests.submit("bob", typeId("Laptop"), "second");
        assertEquals(RequestStatus.REJECTED_BY_POLICY, second.status());
        assertTrue(second.decisionComment().contains("Entitlement limit"));
    }

    @Test
    void policy_blocksItemsAboveBudgetCeiling() {
        RequestView r = requests.submit("carol", typeId("GPU"), "training models");
        assertEquals(RequestStatus.REJECTED_BY_POLICY, r.status());
        assertTrue(r.decisionComment().contains("ceiling"));
    }

    @Test
    void onlyTheRequestersManagerMayApprove() {
        RequestView r = requests.submit("bob", typeId("Monitor"), "dual screen");
        assertThrows(ForbiddenOperationException.class, () -> requests.approve(r.id(), "carol", null)); // peer
        assertThrows(ForbiddenOperationException.class, () -> requests.approve(r.id(), "bob", null));   // self
        assertEquals(RequestStatus.APPROVED, requests.approve(r.id(), "alice", null).status());
    }

    @Test
    void stateMachine_rejectsIllegalTransitions() {
        RequestView r = requests.submit("bob", typeId("Monitor"), "x");
        assertThrows(BusinessRuleException.class, () -> fulfillment.assign(r.id(), "ian")); // not approved yet
        requests.reject(r.id(), "alice", "no budget");
        assertThrows(BusinessRuleException.class, () -> requests.approve(r.id(), "alice", null)); // already rejected
    }

    @Test
    void outOfStock_isBackordered_thenAutoFulfilledWhenStockArrives() {
        ResourceType tablet = types.save(new ResourceType("Test Tablet", ResourceCategory.EQUIPMENT, new BigDecimal("300"), 1));

        RequestView r = requests.submit("bob", tablet.getId(), "field work");
        requests.approve(r.id(), "alice", null);
        assertEquals(RequestStatus.BACKORDERED, fulfillment.assign(r.id(), "ian").status());

        assetService.register(tablet.getId(), "TAB-001", "ian"); // stock arrives -> FIFO backorder served

        RequestView after = requests.get(r.id(), "bob");
        assertEquals(RequestStatus.FULFILLED, after.status());
        assertEquals("TAB-001", after.assignedAssetTag());
    }
}
