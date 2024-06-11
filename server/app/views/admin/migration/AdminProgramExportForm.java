package views.admin.migration;

import com.google.common.collect.ImmutableList;

/** Form used to store which program an admin wants to export. See {@link AdminExportView}. */
public final class AdminProgramExportForm {
  public static final String PROGRAM_ID_FIELD = "programId";
  public static final String PROGRAM_JSON_FIELD = "programJson";
  public static final ImmutableList<String> FIELD_NAMES =
      ImmutableList.of(PROGRAM_ID_FIELD, PROGRAM_JSON_FIELD);

  private Long programId;
  private String programJson;

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramExportForm() {}

  public AdminProgramExportForm(long programId) {
    this.programId = programId;
  }

  public Long getProgramId() {
    return this.programId;
  }

  public String getProgramJson() {
    return this.programJson;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramId(Long programId) {
    this.programId = programId;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramJson(String programJson) {
    this.programJson = programJson;
  }
}
