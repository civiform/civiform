package views.admin.migration;

import com.google.common.collect.ImmutableList;

/**
 * Form used to store JSON representing a program that an admin wants to import. See {@link
 * AdminImportView}.
 */
public final class AdminProgramImportForm {
  public static final String PROGRAM_JSON_FIELD = "programJson";
  public static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(PROGRAM_JSON_FIELD);

  private String programJson;

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramImportForm() {}

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramImportForm(String programJson) {
    this.programJson = programJson;
  }

  public String getProgramJson() {
    return this.programJson;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramJson(String programJson) {
    this.programJson = programJson;
  }
}
