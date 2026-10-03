package com.trackflow.delivery.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.trackflow.delivery.domain.Courier;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    List<Courier> findAllByOrderByNameAsc();

    boolean existsByUsername(String username);

    /** The available courier who has waited longest since their last job, so work is spread evenly. */
    @Query("""
            select c from Courier c
            where c.available = true
            order by c.lastAssignedAt asc nulls first, c.name asc
            limit 1
            """)
    Optional<Courier> findNextAvailable();
}
