# --- Add localized summary image descriptions for programs.

# --- !Ups
alter table programs add column localized_summary_image_description jsonb;

# --- !Downs
alter table programs drop column localized_summary_image_description;
