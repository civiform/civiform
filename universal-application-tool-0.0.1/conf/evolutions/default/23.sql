# --- Migrate the old column for localization instead of creating a new one.

# --- !Ups

alter table questions drop column question_options jsonb;
alter table questions rename column question_options_with_locales to question_options;

# --- !Downs

alter table questions rename column question_options to question_options_with_locales;
alter table questions add column question_options jsonb;
