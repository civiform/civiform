# --- S3 file table

# --- !Ups
create table if not exists files (
  id bigserial primary key,
  name varchar(255)
);

# --- !Downs

drop table if exists files cascade;
