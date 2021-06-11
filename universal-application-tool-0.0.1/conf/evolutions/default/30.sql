# --- !Ups
alter table questions add column if not exists question_tags varchar[];
create index if not exists idx_question_tags on questions using gin(question_tags);

# --- !Downs
drop index if exists idx_question_tags;
alter table questions drop column if exists question_tags;