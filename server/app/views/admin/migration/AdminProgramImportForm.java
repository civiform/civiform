package views.admin.migration;

import com.google.common.collect.ImmutableList;

/**
 * Form used to store JSON representing a program that an admin wants to import. See {@link
 * AdminImportView}.
 */
public final class AdminProgramImportForm {
  public static final String PROGRAM_JSON_FIELD = "programJSON";
  public static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(PROGRAM_JSON_FIELD);

  private String programJSON;

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramImportForm() {}

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramImportForm(String programJSON) {
    this.programJSON = programJSON;
  }

  public String getProgramJSON() {
    return this.programJSON;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramJSON(String programJSON) {
    this.programJSON = programJSON;
  }
}
