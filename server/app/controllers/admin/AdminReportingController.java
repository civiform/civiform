package controllers.admin;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import controllers.BadRequestException;
import controllers.CiviFormController;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Provider;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.reporting.ReportingService;
import views.admin.reporting.AdminReportingIndexView;
import views.admin.reporting.AdminReportingShowView;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final Provider<AdminReportingIndexView> adminReportingIndexView;
  private final Provider<AdminReportingShowView> adminReportingShowView;
  private final ReportingService reportingService;
  private final ProgramRepository programRepository;

  @Inject
  public AdminReportingController(
      Provider<AdminReportingIndexView> adminReportingIndexView,
      Provider<AdminReportingShowView> adminReportingShowView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ReportingService reportingService,
      ProgramRepository programRepository) {
    super(profileUtils, versionRepository);
    this.adminReportingIndexView = Preconditions.checkNotNull(adminReportingIndexView);
    this.adminReportingShowView = Preconditions.checkNotNull(adminReportingShowView);
    this.reportingService = Preconditions.checkNotNull(reportingService);
    this.programRepository = Preconditions.checkNotNull(programRepository);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(Http.Request request) {
    return ok(
        adminReportingIndexView
            .get()
            .render(request, getCiviFormProfile(request), reportingService.getMonthlyStats()));
  }

  private CiviFormProfile getCiviFormProfile(Http.Request request) {
    return profileUtils
        .currentUserProfile(request)
        .orElseThrow(() -> new RuntimeException("User authorized as admin but no profile found."));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result show(Http.Request request, String programSlug) {
    String programLocalizedName =
        programRepository
            .getShallowProgramDefinition(
                programRepository.getActiveProgramFromSlug(programSlug).join())
            .localizedName()
            .getDefault();
    String programAdminName =
        programRepository
            .getShallowProgramDefinition(
                programRepository.getActiveProgramFromSlug(programSlug).join())
            .adminName();
    return ok(
        adminReportingShowView
            .get()
            .render(
                request,
                getCiviFormProfile(request),
                programSlug,
                programAdminName,
                programLocalizedName,
                reportingService.getMonthlyStats()));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result downloadProgramCsv(String programSlug) {
    String programName =
        programRepository
            .getActiveProgramFromSlug(programSlug)
            .toCompletableFuture()
            .join()
            .getProgramDefinition()
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
                String.format("CiviForm_%s.csv", dataSetName.toLowerCase(Locale.ROOT))));
  }
}
