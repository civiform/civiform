package models;

/**
 * Represents the tabs available to Civiform admins in the program list.
 *
 * <p>Tabs:
 *
 * <ul>
 *   <li>{@link #IN_USE}: Displays a list of programs currently in use.
 *   <li>{@link #DISABLED}: Displays a list of disabled programs.
 * </ul>
 */
public enum ProgramTab {
  IN_USE("In use"),
  DISABLED("Disabled");

  private final String tab;

  ProgramTab(String tab) {
    this.tab = tab;
  }

  public String getValue() {
    return this.tab;
  }
}
