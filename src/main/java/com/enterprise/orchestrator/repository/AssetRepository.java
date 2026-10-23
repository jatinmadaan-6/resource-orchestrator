package com.enterprise.orchestrator.repository;

import com.enterprise.orchestrator.domain.AppUser;
import com.enterprise.orchestrator.domain.Asset;
import com.enterprise.orchestrator.domain.AssetStatus;
import com.enterprise.orchestrator.domain.ResourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByAssetTag(String assetTag);

    long countByResourceType(ResourceType type);

    long countByResourceTypeAndStatus(ResourceType type, AssetStatus status);

    long countByAssignedToAndResourceTypeAndStatus(AppUser user, ResourceType type, AssetStatus status);

    List<Asset> findByAssignedToOrderByAssignedAtDesc(AppUser user);

    /**
     * SELECT ... FOR UPDATE. Two IT admins assigning at the same instant will queue on this lock,
     * so the same laptop can never be handed to two people.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.resourceType = :type and a.status = :status order by a.id")
    List<Asset> findAvailableForUpdate(@Param("type") ResourceType type,
                                       @Param("status") AssetStatus status,
                                       Pageable pageable);
}
