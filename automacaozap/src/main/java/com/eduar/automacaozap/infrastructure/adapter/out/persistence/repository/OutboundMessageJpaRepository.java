package com.eduar.automacaozap.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import com.eduar.automacaozap.infrastructure.adapter.out.persistence.entity.OutboundMessageJpaEntity;
import org.springframework.transaction.annotation.Transactional;

public interface OutboundMessageJpaRepository extends JpaRepository<OutboundMessageJpaEntity, Long> {

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from OutboundMessageJpaEntity o where o.status = 'PENDING' order by o.createdAt asc")
    List<OutboundMessageJpaEntity> findPendingBatch(Pageable pageable);
}
