# --- !Ups
alter table if exists questions
add column if not exists display_mode varchar(32) not null default 'VISIBLE';

alter table if exists questions
alter column display_mode DROP DEFAULT;

# --- !Downs
alter table if exists questions
drop column if exists display_mode;
