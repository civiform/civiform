# --- !Ups

alter table programs add column localized_short_description jsonb, add column application_steps jsonb;

# --- !Downs

alter table programs drop column if exists localized_short_description, application_steps;