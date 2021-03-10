# --- !Ups

alter table accounts add email_address varchar;

# --- !Downs

alter table accounts drop column email_address;