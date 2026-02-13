package mapping.admin.reporting;

import services.reporting.ReportingService.MonthlyStats;
import views.admin.reporting.AdminReportingPageViewModel;

/** Maps reporting data to the AdminReportingPageViewModel. */
public final class AdminReportingPageMapper {

  public AdminReportingPageViewModel map(MonthlyStats monthlyStats) {
    return AdminReportingPageViewModel.builder().monthlyStats(monthlyStats).build();
  }
}
