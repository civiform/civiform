# --- !Ups
alter table if exists programs
add column if not exists bridge_definitions jsonb not null default '[]';

# --- !Downs
alter table programs
drop column if exists bridge_definitions;
