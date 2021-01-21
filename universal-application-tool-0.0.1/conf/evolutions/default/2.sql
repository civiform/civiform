# --- Sample dataset

# --- !Ups

insert into person (id,name) values (  1,'Alice');
insert into person (id,name) values (  2,'Bob');
insert into person (id,name) values (  3,'Charles');
insert into person (id,name) values (  4,'Diana');
insert into person (id,name) values (  5,'Eliza');

# --- !Downs

delete from person;