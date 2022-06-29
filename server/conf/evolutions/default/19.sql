# --- Rename questions.repeater_id to questions.enumerator_id (from evolution 13)

# --- !Ups
alter table questions rename column repeater_id to enumerator_id;

# --- !Downs
alter table questions rename column enumerator_id to repeater_id;
