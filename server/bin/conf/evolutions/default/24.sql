# --- Enumerator entity type (localized)

# --- !Ups

alter table questions add enumerator_entity_type jsonb;

# --- !Downs

alter table questions drop column if exists enumerator_entity_type;