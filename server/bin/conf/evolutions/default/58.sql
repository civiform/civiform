# --- !Ups

alter table accounts add column id_tokens jsonb;

# --- !Downs

alter table accounts drop column if exists id_tokens;
