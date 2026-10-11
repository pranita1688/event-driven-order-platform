create table orders (
    id             uuid           primary key,
    sku            varchar(64)    not null,
    quantity       integer        not null check (quantity > 0),
    amount         numeric(12, 2) not null check (amount > 0),
    status         varchar(20)    not null,
    failure_reason varchar(255),
    created_at     timestamptz    not null,
    version        bigint         not null default 0
);

create index idx_orders_status on orders (status);
