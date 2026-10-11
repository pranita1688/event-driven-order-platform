create table stock_item (
    sku       varchar(64) primary key,
    available integer     not null check (available >= 0),
    version   bigint      not null default 0
);

create table reservation (
    order_id uuid        primary key,
    sku      varchar(64) not null,
    quantity integer     not null,
    status   varchar(20) not null
);

insert into stock_item (sku, available) values
    ('LAPTOP', 10),
    ('PHONE', 50),
    ('HEADSET', 100);
