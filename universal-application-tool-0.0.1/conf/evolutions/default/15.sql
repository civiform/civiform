# --- Add localized name and description to the Programs table.

# --- !Ups

alter table programs add localized_name jsonb;
alter table programs add localized_description jsonb;

# --- !Downs

alter table programs drop column localized_name;
alter table programs drop column localized_description;
