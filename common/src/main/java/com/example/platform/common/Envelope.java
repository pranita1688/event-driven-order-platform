package com.example.platform.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/** Wire format of every message on Kafka. {@code eventId} is the idempotency key. */
public record Envelope(UUID eventId, String type, String aggregateId, JsonNode payload) {
}
