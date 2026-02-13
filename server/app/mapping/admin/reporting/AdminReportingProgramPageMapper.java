package mapping.admin.reporting;

import com.google.common.collect.ImmutableList;
import services.reporting.ApplicationSubmissionsStat;
import views.admin.reporting.AdminReportingProgramPageViewModel;

/** Maps program reporting data to the AdminReportingProgramPageViewModel. */
public final class AdminReportingProgramPageMapper {

  public AdminReportingProgramPageViewModel map(
      ImmutableList<ApplicationSubmissionsStat> monthlySubmissionsForProgram,
      String programSlug,
      String programName,
      String enUSLocalizedProgramName) {
    return AdminReportingProgramPageViewModel.builder()
        .monthlySubmissionsForProgram(monthlySubmissionsForProgram)
        .programSlug(programSlug)
        .programName(programName)
        .enUSLocalizedProgramName(enUSLocalizedProgramName)
        .build();
  }
}
