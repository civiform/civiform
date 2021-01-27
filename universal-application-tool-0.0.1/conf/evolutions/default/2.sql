# --- Sample dataset

# --- !Ups

insert into person (name) values ('Alice');
insert into person (name) values ('Bob');
insert into person (name) values ('Charles');
insert into person (name) values ('Diana');
insert into person (name) values ('Eliza');

# --- !Downs

delete from person;