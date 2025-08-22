package controllers.admin;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import controllers.BadRequestException;
import controllers.CiviFormController;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramService;
import services.reporting.ReportingService;
import views.admin.reporting.AdminReportingIndexView;
import views.admin.reporting.AdminReportingShowView;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final Provider<AdminReportingIndexView> adminReportingIndexView;
  private final Provider<AdminReportingShowView> adminReportingShowView;
  private final ProgramService programService;
  private final ReportingService reportingService;

  @Inject
  public AdminReportingController(
      Provider<AdminReportingIndexView> adminReportingIndexView,
      Provider<AdminReportingShowView> adminReportingShowView,
      ProfileUtils profileUtils,
      ProgramService programService,
      VersionRepository versionRepository,
      ReportingService reportingService) {
    super(profileUtils, versionRepository);
    this.adminReportingIndexView = Preconditions.checkNotNull(adminReportingIndexView);
    this.adminReportingShowView = Preconditions.checkNotNull(adminReportingShowView);
    this.programService = Preconditions.checkNotNull(programService);
    this.reportingService = Preconditions.checkNotNull(reportingService);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(Http.Request request) {
    return ok(
        adminReportingIndexView
            .get()
            .render(
                request,
                profileUtils.currentUserProfile(request),
                reportingService.getMonthlyStats()));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public CompletionStage<Result> show(Http.Request request, String programSlug) {
    return programService
        .getActiveFullProgramDefinitionAsync(programSlug)
        .thenApply(
            programDefinition ->
                ok(
                    adminReportingShowView
                        .get()
                        .render(
                            request,
                            profileUtils.currentUserProfile(request),
                            programSlug,
                            programDefinition.adminName(),
                            programDefinition.localizedName().getDefault(),
                            reportingService.getMonthlyStats())));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadProgramCsv(String programSlug) {
    String programName =
        programService
            .getActiveFullProgramDefinitionAsync(programSlug)
            .toCompletableFuture()
            .join()
            .adminName();

    String csv = reportingService.applicationsToProgramByMonthCsv(programName);

    return ok(csv)
        .as(Http.MimeTypes.BINARY)
        .withHeader(
            "Content-Disposition",
            String.format(
                "attachment; filename=\"%s\"", String.format("CiviForm_%s.csv", programSlug)));
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
      csv =
          switch (DataSetName.valueOf(dataSetName)) {
            case APPLICATION_COUNTS_BY_MONTH -> reportingService.applicationCountsByMonthCsv();
            case APPLICATION_COUNTS_BY_PROGRAM -> reportingService.applicationCountsByProgramCsv();
            default -> throw new BadRequestException("Unrecognized DataSetName: " + dataSetName);
          };
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BadRequestException(e.getMessage());
    }

    return ok(csv)
        .as(Http.MimeTypes.BINARY)
        .withHeader(
            "Content-Disposition",
            String.format(
                "attachment; filename=\"%s\"",
                String.format("CiviForm_%s.csv", dataSetName.toLowerCase(Locale.ROOT))));
  }
}
