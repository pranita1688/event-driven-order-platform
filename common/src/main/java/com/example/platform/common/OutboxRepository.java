package com.example.platform.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /** SKIP LOCKED lets several instances of a service relay the outbox without blocking each other. */
    @Query(value = """
            select * from outbox_event
            where published_at is null
            order by created_at
            limit 100
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch();
}
