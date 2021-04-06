# --- Add answer options for multi-option questions
# --- !Ups

alter table applicants add when_created timestamp;

update applicants set when_created = current_timestamp where when_created is null;

update programs set export_definitions = '[]' where export_definitions is null;

# --- !Downs

alter table applicants drop column when_created;
