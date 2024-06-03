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

create table corporations
(
    consumer_record_id uuid                     default gen_random_uuid() not null
        primary key,
    name               varchar(320)                                       not null,
    name_katakana      varchar(320)                                       not null,
    created_at         timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at         timestamp with time zone default CURRENT_TIMESTAMP
);

create table natural_persons
(
    consumer_record_id uuid                     default gen_random_uuid()                           not null
        primary key,
    name               varchar(320)                                                                 not null,
    name_katakana      varchar(320)                                                                 not null,
    date_of_birth      date                                                                         not null,
    sex                integer                                                                      not null,
    created_at         timestamp with time zone default CURRENT_TIMESTAMP,
    updated_at         timestamp with time zone default CURRENT_TIMESTAMP
);
