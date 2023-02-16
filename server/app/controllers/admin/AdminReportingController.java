package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import services.reporting.ReportingService;
import views.admin.reporting.AdminReportingIndexView;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final AdminReportingIndexView adminReportingIndexView;
  private final ReportingService reportingService;

  @Inject
  public AdminReportingController(
      AdminReportingIndexView adminReportingIndexView, ReportingService reportingService) {
    this.adminReportingIndexView = Preconditions.checkNotNull(adminReportingIndexView);
    this.reportingService = Preconditions.checkNotNull(reportingService);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index() {
    return ok(adminReportingIndexView.render(reportingService.getMonthlyStats()));
  }
}
