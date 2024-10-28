# --- !Ups

alter table programs add localized_short_description jsonb;
alter table programs add application_steps jsonb;

update programs set localized_short_description = ('{"isRequired": true, "translations": {"en_US": ""}}')::jsonb where localized_short_description is null;
update programs set application_steps = ('[]')::jsonb where application_steps is null;

# --- !Downs

alter table programs drop column if exists localized_short_description, application_steps;