package services.export.enums;

/** Options values for a multiselect export */
public enum MultiOptionSelectionExportType {
  // Option in the selected list and in the question definition and the question is answered
  SELECTED,
  // Option not in the selected list and in the question definition and question is answered
  NOT_SELECTED,
  // Option not int the selected list and in the question definition and question is not answered
  NOT_ANSWERED,
  // Option not in the selected list and not in the question definition
  // Covers both 'retired' and 'not yet an option at the time of this application' cases
  NOT_AN_OPTION_AT_PROGRAM_VERSION;
}
