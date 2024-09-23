# --- !Ups

alter table accounts add column active_sessions jsonb;

# --- !Downs

alter table accounts drop column if exists active_sessions;
