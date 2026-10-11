package com.example.platform.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EventCodec {

    private final ObjectMapper mapper;

    public EventCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Envelope wrap(String type, String aggregateId, Object payload) {
        return new Envelope(java.util.UUID.randomUUID(), type, aggregateId, mapper.valueToTree(payload));
    }

    public String toJson(Envelope envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + envelope.type(), e);
        }
    }

    public Envelope parse(String json) {
        try {
            return mapper.readValue(json, Envelope.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed event: " + json, e);
        }
    }

    public <T> T payload(Envelope envelope, Class<T> type) {
        try {
            return mapper.treeToValue(envelope.payload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read " + envelope.type() + " payload", e);
        }
    }
}
