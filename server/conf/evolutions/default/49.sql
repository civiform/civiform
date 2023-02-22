# --- !Ups

alter table programs add column localized_confirmation_message jsonb;

# --- !Downs

alter table programs drop column if exists localized_confirmation_message;
