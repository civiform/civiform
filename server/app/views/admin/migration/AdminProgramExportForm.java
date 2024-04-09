package views.admin.migration;

import com.google.common.collect.ImmutableList;

/** Form used to store which program an admin wants to export. See {@link AdminExportView}. */
public final class AdminProgramExportForm {
  public static final String PROGRAM_ID_FIELD = "programId";
  public static final ImmutableList<String> FIELD_NAMES = ImmutableList.of(PROGRAM_ID_FIELD);

  private Long programId;

  @SuppressWarnings("unused") // Used by FormFactory
  public AdminProgramExportForm() {}

  public AdminProgramExportForm(long programId) {
    this.programId = programId;
  }

  public Long getProgramId() {
    return this.programId;
  }

  @SuppressWarnings("unused") // Used by FormFactory
  public void setProgramId(Long programId) {
    this.programId = programId;
  }
}
