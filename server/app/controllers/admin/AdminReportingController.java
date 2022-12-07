package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Result;
import views.admin.reporting.AdminReportingIndexView;

/** Controller for displaying reporting data to the admin roles. */
public final class AdminReportingController extends CiviFormController {

  private final AdminReportingIndexView adminReportingIndexView;

  @Inject
  public AdminReportingController(AdminReportingIndexView adminReportingIndexView) {
    this.adminReportingIndexView = Preconditions.checkNotNull(adminReportingIndexView);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index() {
    return ok(
        adminReportingIndexView.render(
            fakeAllApplicationsMonthlyStats(), fakeAggregatedProgramStats()));
  }

  private ImmutableList<AdminReportingIndexView.AppplicationsSubmittedStat>
      fakeAllApplicationsMonthlyStats() {
    return ImmutableList.of(
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "All", "All", 2757, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "All", "12/2022", 831, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "All", "11/2022", 701, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "All", "10/2022", 672, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "All", "9/2022", 553, "5:20", "8:20", "10:10", "20:11"));
  }

  private ImmutableList<AdminReportingIndexView.AppplicationsSubmittedStat>
      fakeAggregatedProgramStats() {
    return ImmutableList.of(
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "Senior mobility program", "All", 2757, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "Afterschool childcare program", "All", 831, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "Forestry job training program", "All", 701, "5:20", "8:20", "10:10", "20:11"),
        AdminReportingIndexView.AppplicationsSubmittedStat.create(
            "SNAP benefits", "All", 672, "5:20", "8:20", "10:10", "20:11"));
  }
}
