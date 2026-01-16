# --- !Ups
alter table versions add column if not exists tombstoned_question_names varchar[];
alter table versions add column if not exists tombstoned_program_names varchar[];

# --- !Downs
alter table versions drop column if exists tombstoned_program_names;
alter table versions drop column if exists tombstoned_question_names;
