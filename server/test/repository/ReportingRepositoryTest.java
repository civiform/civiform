package repository;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.ProgramModel;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.Test;
import services.reporting.ApplicationSubmissionsStat;
import support.ProgramBuilder;

public class ReportingRepositoryTest extends ResetPostgres {

  private ReportingRepository repo;
  private Applicant applicant;
  private ProgramModel programA;
  private ProgramModel programB;

  @Before
  public void setUp() {
    repo = new ReportingRepository(testClock);
    applicant = resourceCreator.insertApplicantWithAccount();
    programA = ProgramBuilder.newActiveProgram().withName("Fake Program A").build();
    programB = ProgramBuilder.newActiveProgram().withName("Fake Program B").build();
  }

  @Test
  public void monthlyReportingView() {
    Instant lastMonth = testClock.instant().minus(40, ChronoUnit.DAYS);
    Instant twoMonthsAgo = testClock.instant().minus(70, ChronoUnit.DAYS);

    ImmutableList.of(
            Triple.of(LifecycleStage.ACTIVE, lastMonth, lastMonth.plusSeconds(100)),
            Triple.of(LifecycleStage.OBSOLETE, lastMonth, lastMonth.plusSeconds(1000)),
            Triple.of(LifecycleStage.OBSOLETE, lastMonth, lastMonth.plusSeconds(500)),
            Triple.of(LifecycleStage.DRAFT, lastMonth, lastMonth.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programA,
                    applicationSpec.getLeft(),
                    applicationSpec.getMiddle(),
                    applicationSpec.getRight()));

    ImmutableList.of(
            Triple.of(LifecycleStage.ACTIVE, twoMonthsAgo, twoMonthsAgo.plusSeconds(100)),
            Triple.of(LifecycleStage.OBSOLETE, twoMonthsAgo, twoMonthsAgo.plusSeconds(1000)),
            Triple.of(LifecycleStage.OBSOLETE, twoMonthsAgo, twoMonthsAgo.plusSeconds(500)),
            Triple.of(LifecycleStage.DRAFT, twoMonthsAgo, twoMonthsAgo.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programB,
                    applicationSpec.getLeft(),
                    applicationSpec.getMiddle(),
                    applicationSpec.getRight()));

    repo.refreshMonthlyReportingView();

    assertThat(repo.loadMonthlyReportingView())
        .containsExactly(
            // The expected values here have submission duration percentile stats calculated from
            // the submitted (i.e. active and obsolete) applications.
            ApplicationSubmissionsStat.create(
                "Fake Program A", getMonthTimestamp(lastMonth), 3L, 300, 500, 750, 990),
            ApplicationSubmissionsStat.create(
                "Fake Program B", getMonthTimestamp(twoMonthsAgo), 3L, 300, 500, 750, 990));
  }

  @Test
  public void loadThisMonthReportingData() {
    Instant today = testClock.instant();

    ImmutableList.of(
            Triple.of(LifecycleStage.ACTIVE, today, today.plusSeconds(100)),
            Triple.of(LifecycleStage.OBSOLETE, today, today.plusSeconds(1000)),
            Triple.of(LifecycleStage.OBSOLETE, today, today.plusSeconds(500)),
            Triple.of(LifecycleStage.DRAFT, today, today.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programA,
                    applicationSpec.getLeft(),
                    applicationSpec.getMiddle(),
                    applicationSpec.getRight()));

    ImmutableList.of(
            Triple.of(LifecycleStage.ACTIVE, today, today.plusSeconds(100)),
            Triple.of(LifecycleStage.OBSOLETE, today, today.plusSeconds(1000)),
            Triple.of(LifecycleStage.OBSOLETE, today, today.plusSeconds(500)),
            Triple.of(LifecycleStage.DRAFT, today, today.plusSeconds(1)))
        .stream()
        .forEach(
            applicationSpec ->
                createFakeApplication(
                    programB,
                    applicationSpec.getLeft(),
                    applicationSpec.getMiddle(),
                    applicationSpec.getRight()));

    assertThat(repo.loadThisMonthReportingData())
        .containsExactly(
            // The expected values here have submission duration percentile stats calculated from
            // the submitted (i.e. active and obsolete) applications.
            ApplicationSubmissionsStat.create(
                "Fake Program A", getMonthTimestamp(today), 3L, 300, 500, 750, 990),
            ApplicationSubmissionsStat.create(
                "Fake Program B", getMonthTimestamp(today), 3L, 300, 500, 750, 990));
  }

  private static Optional<Timestamp> getMonthTimestamp(Instant lastMonth) {
    return Optional.of(
        Timestamp.from(
            lastMonth.atZone(UTC).truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant()));
  }

  private Application createFakeApplication(
      ProgramModel program, LifecycleStage lifecycleStage, Instant createTime, Instant submitTime) {
    Application application = new Application(applicant, program, lifecycleStage);
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
