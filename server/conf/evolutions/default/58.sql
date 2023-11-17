# --- !Ups

alter table accounts add ti_note varchar;

# --- #!Downs

alter table accounts drop column ti_note;
alter table accounts drop column ti_archived_at;
