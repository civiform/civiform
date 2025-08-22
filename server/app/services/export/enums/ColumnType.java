package services.export.enums;

/**
 * Defines types of columns that will be handled differently in {@link services.export.CsvExporter}.
 */
public enum ColumnType {
  APPLICANT_ANSWER,
  APPLICANT_ID,
  APPLICATION_ID,
  LANGUAGE,
  SUBMIT_TIME,
  TI_EMAIL,
  TI_EMAIL_OPAQUE,
  OPAQUE_ID,
  APPLICANT_OPAQUE,
  SUBMITTER_TYPE,
  PROGRAM,
  TI_ORGANIZATION,
  CREATE_TIME,
  ELIGIBILITY_STATUS,
  STATUS_TEXT,
  ADMIN_NOTE,
  STATUS_LAST_MODIFIED_TIME
}
