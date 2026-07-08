package com.ordermgmt.pipeline.domain;

import com.ordermgmt.common.domain.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByStateInAndUpdatedAtBefore(Collection<OrderState> states, Instant threshold);
}
