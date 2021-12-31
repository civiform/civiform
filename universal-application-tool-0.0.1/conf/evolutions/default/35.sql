# --- Store optional user_file_name with file names

# --- !Ups
alter table files add column user_file_name varchar;

# --- !Downs
alter table files drop column user_file_name;