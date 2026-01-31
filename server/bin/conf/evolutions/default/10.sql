# --- Add answer options for multi-option questions
# --- !Ups

alter table questions add question_options jsonb;

# --- !Downs

alter table questions drop column question_options;