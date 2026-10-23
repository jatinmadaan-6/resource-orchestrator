package com.enterprise.orchestrator.repository;

import com.enterprise.orchestrator.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, Long> {

    List<ResourceRequest> findByRequesterOrderByCreatedAtDesc(AppUser requester);

    List<ResourceRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    List<ResourceRequest> findByStatusInOrderByCreatedAtAsc(Collection<RequestStatus> statuses);

    /** FIFO queue of requests waiting for stock of a given type. */
    List<ResourceRequest> findByResourceTypeAndStatusOrderByCreatedAtAsc(ResourceType type, RequestStatus status);

    long countByRequesterAndResourceTypeAndStatusIn(AppUser requester, ResourceType type,
                                                    Collection<RequestStatus> statuses);

    /** A manager's inbox: pending requests from their direct reports. */
    @Query("select r from ResourceRequest r where r.status = :status and r.requester.manager = :manager order by r.createdAt")
    List<ResourceRequest> findForManager(@Param("manager") AppUser manager, @Param("status") RequestStatus status);
}
