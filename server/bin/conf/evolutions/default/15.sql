# --- Add answer options for multi-option questions
# --- !Ups

alter table questions add question_options_with_locales jsonb;

# --- !Downs

alter table questions drop column question_options_with_locales;
