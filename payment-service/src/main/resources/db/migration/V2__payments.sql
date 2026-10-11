create table payment (
    order_id   uuid           primary key,
    amount     numeric(12, 2) not null,
    status     varchar(20)    not null,
    reason     varchar(255),
    created_at timestamptz    not null
);
