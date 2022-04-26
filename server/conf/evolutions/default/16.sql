# --- Add localized name and description to the Programs table.

# --- !Ups

alter table programs add localized_name jsonb;
alter table programs add localized_description jsonb;

update programs set localized_name = ('{"en_US" : "' || name || '"}')::jsonb where localized_name is null;
update programs set localized_description = ('{"en_US" : "' || description || '"}')::jsonb where localized_description is null;

# --- !Downs

alter table programs drop column localized_name;
alter table programs drop column localized_description;
