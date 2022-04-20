# --- Allow programs to specify their display mode

# --- !Ups
alter table programs add column display_mode varchar default 'PUBLIC' not null;

# --- !Downs
alter table programs drop column display_mode;
