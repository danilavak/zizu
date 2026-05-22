alter table user_accounts
    add column if not exists account_expired boolean not null default false;

alter table user_accounts
    add column if not exists account_locked boolean not null default false;

alter table user_accounts
    add column if not exists credentials_expired boolean not null default false;

create table if not exists products (
    id bigserial primary key,
    name varchar(120) not null,
    blocked boolean not null default false,
    constraint uk_products_name unique (name)
);

create table if not exists license_types (
    id bigserial primary key,
    name varchar(40) not null,
    default_duration_in_days integer not null,
    description varchar(255),
    constraint uk_license_types_name unique (name)
);

create table if not exists licenses (
    id bigserial primary key,
    code varchar(64) not null,
    product_id bigint not null,
    type_id bigint not null,
    owner_id bigint not null,
    user_id bigint,
    first_activation_date timestamp with time zone,
    ending_date timestamp with time zone,
    blocked boolean not null default false,
    device_count integer not null,
    description varchar(255),
    constraint uk_licenses_code unique (code),
    constraint fk_licenses_product foreign key (product_id) references products (id),
    constraint fk_licenses_type foreign key (type_id) references license_types (id),
    constraint fk_licenses_owner foreign key (owner_id) references user_accounts (id),
    constraint fk_licenses_user foreign key (user_id) references user_accounts (id)
);

create table if not exists devices (
    id bigserial primary key,
    name varchar(120) not null,
    mac_address varchar(64) not null,
    user_account_id bigint not null,
    constraint uk_devices_mac_address unique (mac_address),
    constraint fk_devices_user foreign key (user_account_id) references user_accounts (id)
);

create table if not exists device_licenses (
    id bigserial primary key,
    license_id bigint not null,
    device_id bigint not null,
    activated_at timestamp with time zone not null,
    constraint uk_device_licenses_license_device unique (license_id, device_id),
    constraint fk_device_licenses_license foreign key (license_id) references licenses (id),
    constraint fk_device_licenses_device foreign key (device_id) references devices (id)
);

create table if not exists license_history (
    id bigserial primary key,
    license_id bigint not null,
    status varchar(32) not null,
    user_account_id bigint not null,
    created_at timestamp with time zone not null,
    details varchar(255),
    constraint fk_license_history_license foreign key (license_id) references licenses (id),
    constraint fk_license_history_user foreign key (user_account_id) references user_accounts (id)
);

create index if not exists idx_licenses_owner_id on licenses (owner_id);
create index if not exists idx_licenses_user_id on licenses (user_id);
create index if not exists idx_devices_user_account_id on devices (user_account_id);
create index if not exists idx_device_licenses_license_id on device_licenses (license_id);
create index if not exists idx_license_history_license_id on license_history (license_id);

insert into products (id, name, blocked)
select 1, 'Antivirus', false
where not exists (select 1 from products where id = 1);

insert into license_types (id, name, default_duration_in_days, description)
select 1, 'TRIAL', 1, 'Trial license'
where not exists (select 1 from license_types where id = 1);

insert into license_types (id, name, default_duration_in_days, description)
select 2, 'MONTH', 30, 'Monthly license'
where not exists (select 1 from license_types where id = 2);

insert into license_types (id, name, default_duration_in_days, description)
select 3, 'YEAR', 365, 'Yearly license'
where not exists (select 1 from license_types where id = 3);

insert into license_types (id, name, default_duration_in_days, description)
select 4, 'CORPORATE', 365, 'Corporate license'
where not exists (select 1 from license_types where id = 4);
