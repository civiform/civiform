package views.admin.reporting;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;
import modules.MainModule;
import services.reporting.ReportingService.MonthlyStats;
import views.admin.BaseViewModel;

@Data
@Builder
public class AdminReportingPageViewModel implements BaseViewModel {
  private final MonthlyStats monthlyStats;

  public String getDetailsUrl(String programName) {
    return controllers.admin.routes.AdminReportingController.show(
            MainModule.SLUGIFIER.slugify(programName))
        .url();
  }

  public static String formatDuration(double durationSeconds) {
    Duration duration = Duration.ofSeconds((long) durationSeconds);

    long days = duration.toDaysPart();
    long hours = duration.toHoursPart();
    int minutes = duration.toMinutesPart();
    int seconds = duration.toSecondsPart();

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      result.append(days);
      result.append(":");
    }

    result.append(String.format("%02d:%02d:%02d", hours, minutes, seconds));

    return result.toString();
  }

  public String getDownloadMonthCsvUrl(String csvName) {
    return controllers.admin.routes.AdminReportingController.downloadCsv(csvName).url();
  }
}
