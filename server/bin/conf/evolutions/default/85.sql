# --- !Ups
create table if not exists api_bridge_configuration (
    id bigserial primary key not null,
    host_url varchar not null,
    url_path varchar not null,
    admin_name varchar not null
        constraint ck_api_bridge_configuration_admin_name_format check (admin_name ~ '^[a-z][a-z0-9-]*$'),
    compatibility_level varchar not null,
    description varchar not null,
    request_schema jsonb not null,
    request_schema_checksum varchar not null,
    response_schema jsonb not null,
    response_schema_checksum varchar not null,
    global_bridge_definition jsonb null,
    enabled boolean not null,
    create_time timestamp not null,
    update_time timestamp not null,
    constraint uq_api_bridge_configuration_admin_name unique(admin_name),
    constraint uq_api_bridge_configuration_host_url_url_path_compatibility_level unique(host_url, url_path, compatibility_level)
);

# --- !Downs
drop table if exists api_bridge_configuration;
