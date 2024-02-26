package views.admin.programs;

/**
 * An enum representing how an admin got to the program translations page. Used so that the "Back"
 * button on the translations page goes back to the right place.
 */
public enum ProgramTranslationReferer {
  /**
   * The admin came from the program dashboard page (by clicking "Manage translations" on a specific
   * program).
   */
  PROGRAM_DASHBOARD,
  /** The admin came from the program statuses page. */
  PROGRAM_STATUSES,
  /**
   * The admin came from the program image upload page, where they were editing an existing program.
   */
  PROGRAM_IMAGE_UPLOAD_EDIT,
  /**
   * The admin came from the program image upload page, where they were creating a program for the
   * first time.
   */
  PROGRAM_IMAGE_UPLOAD_CREATION;

  /** The referer to use as the default if the provided referer can't be found for some reason. */
  public static final ProgramTranslationReferer DEFAULT_ACTION = PROGRAM_DASHBOARD;
}
