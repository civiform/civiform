# --- !Ups

alter table accounts add email_address varchar;
alter table accounts add unique (email_address);

# --- !Downs

alter table accounts drop column email_address;