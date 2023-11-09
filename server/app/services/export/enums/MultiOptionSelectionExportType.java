package services.export.enums;

/**
 * the four options for a value are:
 *
 * <p>in the selected list and in the question definition and question is answered: SELECTED not in
 * the selected list and in the question definition and question is answered: NOT_SELECTED not in
 * the selected list and in the question definition and question is not answered: NOT_ANSWERED not
 * in the selected list and not in the question definition: NOT_AN_OPTION_AT_PROGRAM_VERSION (or
 * similar. This is both “retired” and “not yet an option at the time of this application”)
 */
public enum MultiOptionSelectionExportType {
  SELECTED,
  NOT_SELECTED,
  NOT_ANSWERED,
  NOT_AN_OPTION_AT_PROGRAM_VERSION;
}
