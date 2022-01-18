# --- Store optional user_file_name with file names

# --- !Ups
alter table files add column original_file_name varchar;

# --- !Downs
alter table files drop column original_file_name;
