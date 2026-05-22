create table if not exists signature_files (
    signature_id uuid primary key,
    object_key varchar(512) not null unique,
    bucket_name varchar(120) not null,
    original_filename varchar(255) not null,
    content_type varchar(255) not null,
    size_bytes bigint not null,
    uploaded_at timestamp with time zone not null,
    constraint fk_signature_files_signature
        foreign key (signature_id) references signatures (id) on delete cascade
);

create index if not exists idx_signature_files_uploaded_at on signature_files (uploaded_at);
