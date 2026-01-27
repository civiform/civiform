package views.admin.reporting;

import java.time.Duration;

import lombok.Builder;
import lombok.Data;
import services.reporting.ReportingService.MonthlyStats;
import views.admin.BaseViewModel;

@Data
@Builder
public class AdminReportingPageViewModel implements BaseViewModel {
	private final MonthlyStats monthlyStats;
	private final String icecreamFlavor;

	public static String renderDuration(double durationSeconds) {
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
}
