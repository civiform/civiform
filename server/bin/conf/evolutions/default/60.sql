# --- Add localized summary image file keys for programs.

# --- !Ups
alter table programs add column summary_image_file_key varchar;

# --- !Downs
alter table programs drop column summary_image_file_key;
