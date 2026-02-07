# --- Remove legacy question columns

# --- !Ups
alter table questions drop column if exists legacy_question_text;
alter table questions drop column if exists legacy_question_help_text;
alter table questions drop column if exists legacy_question_options;

# --- !Downs
alter table questions add column if not exists legacy_question_text jsonb;
alter table questions add column if not exists legacy_question_help_text jsonb;
alter table questions add column if not exists legacy_question_options jsonb;


