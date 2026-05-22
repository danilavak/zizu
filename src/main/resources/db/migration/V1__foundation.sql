create table if not exists user_accounts (
    id bigserial primary key,
    username varchar(80) not null,
    email varchar(120) not null,
    password_hash varchar(255) not null,
    role varchar(40) not null,
    enabled boolean not null default true,
    constraint uk_user_accounts_username unique (username),
    constraint uk_user_accounts_email unique (email)
);

create table if not exists user_sessions (
    id bigserial primary key,
    user_account_id bigint not null,
    refresh_token_id varchar(64) not null,
    status varchar(24) not null,
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    invalidated_at timestamp with time zone,
    constraint fk_user_sessions_user_account foreign key (user_account_id) references user_accounts (id),
    constraint uk_user_sessions_refresh_token_id unique (refresh_token_id)
);

create index if not exists idx_user_sessions_user_account_id on user_sessions (user_account_id);
create index if not exists idx_user_sessions_status on user_sessions (status);
