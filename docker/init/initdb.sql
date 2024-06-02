create table accounts
(
    record_id             serial
        primary key,
    id                    uuid                                                             not null,
    consumer_record_id    uuid,
    valid_from            timestamp with time zone                                         not null,
    valid_to              timestamp with time zone                                         not null,
    transact_from         timestamp with time zone                                         not null,
    transact_to           timestamp with time zone                                         not null,
    tel                   varchar(320),
    postcode              varchar(320),
    address               varchar(320),
    email                 varchar(254)                                                     not null,
    is_email_subscription boolean
);
