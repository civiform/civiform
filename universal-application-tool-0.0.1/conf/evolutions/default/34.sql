# --- Allow programs to be hidden from the applicant home view

# --- !Ups
alter table programs add column hide_from_view boolean default false not null;

# --- !Downs
alter table programs drop column hide_from_view;