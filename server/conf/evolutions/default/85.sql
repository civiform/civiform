# --- !Ups
create table if not exists bridge_configuration (
    id bigserial primary key not null,
    host_uri varchar not null,
    uri_path varchar not null,
    admin_name varchar not null
        constraint ck_bridge_configuration_admin_name_format check (admin_name ~ '^[a-z][a-z0-9-]*$'),
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
    constraint uq_bridge_configuration_admin_name unique(admin_name),
    constraint uq_bridge_configuration_host_uri_uri_path_compatibility_level unique(host_uri, uri_path, compatibility_level)
);

# --- !Downs
drop table if exists bridge_configuration;
