package controllers.admin;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import controllers.BadRequestException;
import controllers.CiviFormController;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramService;
import services.reporting.ReportingService;
import views.admin.reporting.AdminReportingPageView;
import views.admin.reporting.AdminReportingPageViewModel;
import views.admin.reporting.AdminReportingProgramPageView;
import views.admin.reporting.AdminReportingProgramPageViewModel;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final AdminReportingPageView adminReportingPageView;
  private final AdminReportingProgramPageView adminReportingProgramPageView;
  private final ProgramService programService;
  private final ReportingService reportingService;
  private final boolean useDeterministicStats;

  @Inject
  public AdminReportingController(
      AdminReportingPageView adminReportingPageView,
      AdminReportingProgramPageView adminReportingProgramPageView,
      ProfileUtils profileUtils,
      ProgramService programService,
      VersionRepository versionRepository,
      ReportingService reportingService,
      Config config) {
    super(profileUtils, versionRepository);
    this.adminReportingPageView = Preconditions.checkNotNull(adminReportingPageView);
    this.adminReportingProgramPageView = Preconditions.checkNotNull(adminReportingProgramPageView);
    this.programService = Preconditions.checkNotNull(programService);
    this.reportingService = Preconditions.checkNotNull(reportingService);
    this.useDeterministicStats = config.getBoolean("reporting_use_deterministic_stats");
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(Http.Request request) {
    AdminReportingPageViewModel model =
        AdminReportingPageViewModel.builder()
            .monthlyStats(reportingService.getMonthlyStats())
            .useDeterministicStats(useDeterministicStats)
            .build();

    return ok(adminReportingPageView.render(request, model)).as(Http.MimeTypes.HTML);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public CompletionStage<Result> show(Http.Request request, String programSlug) {
    return programService
        .getActiveFullProgramDefinitionAsync(programSlug)
        .thenApply(
            programDefinition -> {
              AdminReportingProgramPageViewModel model =
                  AdminReportingProgramPageViewModel.builder()
                      .monthlySubmissionsForProgram(
                          reportingService
                              .getMonthlyStats()
                              .monthlySubmissionsForProgram(programDefinition.adminName()))
                      .programSlug(programSlug)
                      .programName(programDefinition.adminName())
                      .enUSLocalizedProgramName(programDefinition.localizedName().getDefault())
                      .useDeterministicStats(useDeterministicStats)
                      .build();

              return ok(adminReportingProgramPageView.render(request, model))
                  .as(Http.MimeTypes.HTML);
            });
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
