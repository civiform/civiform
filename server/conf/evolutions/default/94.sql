# --- Add localized summary image file keys for questions.

# --- !Ups
alter table questions add column summary_image_file_key varchar;

# --- !Downs
alter table questions drop column summary_image_file_key;
