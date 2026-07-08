package com.ordermgmt.pipeline.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderHistoryEntity, Long> {
}
