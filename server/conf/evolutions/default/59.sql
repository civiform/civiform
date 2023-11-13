# --- Add summary image information for programs.

# --- !Ups
alter table programs add column summary_image_id bigint;
alter table programs add column localized_summary_image_description jsonb;
alter table programs add foreign key (summary_image_id) references files;

# --- !Downs
alter table programs drop column summary_image_id;
alter table programs drop column localized_summary_image_description;
