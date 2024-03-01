package views.admin.programs;

/**
 * An enum describing where in the creation/edit process an admin is. Used when they load the
 * create/edit program details page.
 */
public enum ProgramEditStatus {
  /** The program is being created for the very first time. */
  CREATION,
  /**
   * The program was just created, and the admin is now editing some details while still in the
   * initial program creation flow.
   */
  CREATION_EDIT,
  /** The program is being edited after the admin has fully finished the creation flow. */
  EDIT;

  public static ProgramEditStatus getStatusFromString(String status) {
    try {
      return ProgramEditStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      return ProgramEditStatus.EDIT;
    }
  }
}
