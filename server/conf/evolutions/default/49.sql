# --- !Ups

alter table programs add column localized_confirmation_screen jsonb;

# --- !Downs

alter table programs drop column if exists localized_confirmation_screen;