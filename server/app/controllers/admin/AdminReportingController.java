package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import controllers.BadRequestException;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramService;
import services.reporting.ReportingService;
import views.admin.reporting.AdminReportingIndexView;
import views.admin.reporting.AdminReportingShowView;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final AdminReportingIndexView adminReportingIndexView;
  private final AdminReportingShowView adminReportingShowView;
  private final ProgramService programService;
  private final ReportingService reportingService;

  @Inject
  public AdminReportingController(
      AdminReportingIndexView adminReportingIndexView,
      AdminReportingShowView adminReportingShowView,
      ProgramService programService,
      ReportingService reportingService) {
    this.adminReportingIndexView = Preconditions.checkNotNull(adminReportingIndexView);
    this.adminReportingShowView = Preconditions.checkNotNull(adminReportingShowView);
    this.programService = Preconditions.checkNotNull(programService);
    this.reportingService = Preconditions.checkNotNull(reportingService);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index() {
    return ok(adminReportingIndexView.render(reportingService.getMonthlyStats()));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result show(String programSlug) {
    String programName =
        programService
            .getActiveProgramDefinitionAsync(programSlug)
            .toCompletableFuture()
            .join()
            .adminName();
    return ok(
        adminReportingShowView.render(
            programSlug, programName, reportingService.getMonthlyStats()));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadProgramCsv(String programSlug) {
    return ok();
  }

  /** Identifiers for the different data sets available for download. */
  public enum DataSetName {
    APPLICATION_COUNTS_BY_PROGRAM,
    APPLICATION_COUNTS_BY_MONTH
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadCsv(String dataSetName) {
    String csv;

    try {
      switch (DataSetName.valueOf(dataSetName)) {
        case APPLICATION_COUNTS_BY_MONTH:
          csv = reportingService.applicationCountsByMonthCsv();
          break;

        case APPLICATION_COUNTS_BY_PROGRAM:
          csv = reportingService.applicationCountsByProgramCsv();
          break;

        default:
          throw new BadRequestException("Unrecognized DataSetName: " + dataSetName);
      }
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BadRequestException(e.getMessage());
    }

    return ok(csv)
        .as(Http.MimeTypes.BINARY)
        .withHeader(
            "Content-Disposition",
            String.format(
                "attachment; filename=\"%s\"",
                String.format("CiviForm_%s.csv", dataSetName.toLowerCase())));
  }
}
