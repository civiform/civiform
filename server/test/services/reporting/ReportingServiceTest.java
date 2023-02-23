package services.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import repository.ReportingRepository;
import repository.ResetPostgres;
import support.ProgramBuilder;

public class ReportingServiceTest extends ResetPostgres {

  private ReportingService service;
  private Applicant applicant;
  private Program programA;
  private Program programB;

  @Before
  public void setUp() {
    service = instanceOf(ReportingService.class);
    applicant = resourceCreator.insertApplicantWithAccount();
    programA = ProgramBuilder.newActiveProgram().withName("Fake Program A").build();
    programB = ProgramBuilder.newActiveProgram().withName("Fake Program B").build();
  }

  @Test
  public void getMonthlyStats_csvViewsOfStats() throws IOException {
    insertFakeApplicationsAndRefreshDatabaseView();

    var parser =
        CSVParser.parse(
            service.applicationCountsByMonthCsv(), CSVFormat.DEFAULT.builder().setHeader().build());

    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Month",
            "Submissions",
            "Time to complete (p25)",
            "Median time to complete",
            "Time to complete (p75)",
            "Time to complete (p99)");

    List<CSVRecord> records = parser.getRecords();
    assertThat(records.get(0).toList())
        .containsExactly("12/2022", "4", "00:05:25", "00:06:40", "00:07:55", "00:09:07");
    assertThat(records.get(1).toList())
        .containsExactly("1/2023", "8", "00:05:25", "00:06:40", "00:07:55", "00:09:07");
    assertThat(records.size()).isEqualTo(2);

    parser =
        CSVParser.parse(
            service.applicationCountsByProgramCsv(),
            CSVFormat.DEFAULT.builder().setHeader().build());
    assertThat(parser.getHeaderNames())
        .containsExactly(
            "Program",
            "Submissions",
            "Time to complete (p25)",
            "Median time to complete",
            "Time to complete (p75)",
            "Time to complete (p99)");

    records = parser.getRecords();
    assertThat(records.get(0).toList())
        .containsExactly("Fake Program B", "6", "00:03:45", "00:04:43", "00:05:41", "00:06:37");
    assertThat(records.get(1).toList())
        .containsExactly("Fake Program A", "6", "00:07:05", "00:08:36", "00:10:08", "00:11:36");
    assertThat(records.size()).isEqualTo(2);
  }

  private void insertFakeApplicationsAndRefreshDatabaseView() {
    Instant lastMonth = Instant.now().minus(40, ChronoUnit.DAYS);
    Instant today = Instant.now();

    ImmutableList.of(
            Pair.of(lastMonth, lastMonth.plusSeconds(100)),
            Pair.of(today, today.plusSeconds(1000)),
            Pair.of(today, today.plusSeconds(500)),
            Pair.of(lastMonth, lastMonth.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programA, applicationSpec.getLeft(), applicationSpec.getRight()));

    ImmutableList.of(
            Pair.of(today, today.plusSeconds(100)),
            Pair.of(lastMonth, lastMonth.plusSeconds(1000)),
            Pair.of(lastMonth, lastMonth.plusSeconds(500)),
            Pair.of(today, today.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programB, applicationSpec.getLeft(), applicationSpec.getRight()));

    instanceOf(ReportingRepository.class).refreshMonthlyReportingView();
  }

  private Application createFakeApplication(
      Program program, Instant createTime, Instant submitTime) {
    Application application = new Application(applicant, program, LifecycleStage.ACTIVE);
    application.setApplicantData(applicant.getApplicantData());
    application.save();

    // CreateTime of an application is set through @onCreate to Instant.now(). To change
    // the value, manually set createTime and save and refresh the application.
    application.setCreateTimeForTest(createTime);
    application.setSubmitTimeForTest(submitTime);
    application.save();

    return application;
  }
}
