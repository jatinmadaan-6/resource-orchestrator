package com.enterprise.orchestrator.repository;

import com.enterprise.orchestrator.domain.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long> { }
