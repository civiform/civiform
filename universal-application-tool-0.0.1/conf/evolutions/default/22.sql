# --- Create new columns for localization, and make the old ones legacy.

# --- !Ups

alter table questions rename column question_text to legacy_question_text;
alter table questions rename column question_help_text to legacy_question_help_text;
alter table questions rename column question_options to legacy_question_options;
alter table questions add question_text jsonb;
alter table questions add question_help_text jsonb;
alter table questions add question_options jsonb;

alter table programs rename column localized_name to legacy_localized_name;
alter table programs rename column localized_description to legacy_localized_description;
alter table programs add localized_name jsonb;
alter table programs add localized_description jsonb;

# --- !Downs

alter table questions drop column if exists question_text;
alter table questions drop column if exists question_help_text;
alter table questions drop column if exists question_options;
alter table questions rename column legacy_question_text to question_text;
alter table questions rename column legacy_question_help_text to question_help_text;
alter table questions rename column legacy_question_options to question_options;

alter table programs drop column if exists localized_name;
alter table programs drop column if exists localized_description;
alter table programs rename column legacy_localized_name to localized_name;
alter table programs rename column legacy_localized_description to localized_description;
