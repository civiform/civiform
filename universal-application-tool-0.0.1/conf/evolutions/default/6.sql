# --- Sample question data

# --- !Ups

insert into applicants (object) values ('"{ \"applicant\": {}, \"metadata\": {} }"');
insert into questions
  (version, path, name, description, question_type, question_text, question_help_text)
  values (1, 'applicant.fav_color', 'Fav color', 'test', 'TEXT', '{ "EN": "Favorite color?" }', '{ "EN": "" }');

insert into programs
  (name, description, block_definitions)
  values ('Test program', 'A program for testing',
  '[ { "id": 1, "name": "Test block", "description": "desc", "questionDefinitions": [{ "id": 1, "version": 1, "name": "Fav color", "path": "applicant.fav_color", "description": "test", "questionText": { "EN": "Favorite color?" }, "questionHelpText": { "EN": "" } }] } ]');

# --- !Downs

delete from applicants;
delete from questions;
delete from programs;
