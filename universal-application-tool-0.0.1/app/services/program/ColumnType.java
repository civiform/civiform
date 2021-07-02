package services.program;

/**
 * Defines types of columns that will be handled differently in {@link services.export.CsvExporter}.
 */
public enum ColumnType {
  APPLICANT,
  ID,
  LANGUAGE,
  SUBMIT_TIME,
  SUBMITTER_EMAIL,
  OPAQUE_ID,
  APPLICANT_OPAQUE,
  PROGRAM,
  TI_ORGANIZATION,
  CREATE_TIME,
  SUBMITTER_EMAIL_OPAQUE;
}
