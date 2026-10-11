-- Transactional outbox: written in the same DB transaction as the business change,
-- then relayed to Kafka by OutboxRelay.
create table outbox_event (
    id           uuid         primary key,
    aggregate_id varchar(64)  not null,
    topic        varchar(100) not null,
    type         varchar(100) not null,
    payload      text         not null,
    created_at   timestamptz  not null,
    published_at timestamptz
);

create index idx_outbox_unpublished on outbox_event (created_at) where published_at is null;

-- Idempotent consumer: an event id is recorded in the same transaction as its side effects.
create table processed_message (
    event_id     uuid        primary key,
    processed_at timestamptz not null
);
