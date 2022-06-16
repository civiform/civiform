package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.Path;

public class ApplicationRepositoryTest extends ResetPostgres {
  private ApplicationRepository repo;
  private DateConverter dateConverter;

  private Version draftVersion;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationRepository.class);
    dateConverter = instanceOf(DateConverter.class);
    draftVersion = new Version(LifecycleStage.DRAFT);
    draftVersion.save();
  }

  @Test
  public void submitApplication_updatesOtherApplicationVersions() {
    Applicant applicant = saveApplicant("Alice");
    Program program = createDraftProgram("Program");

    Application appOne =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    Instant initialSubmitTime = appOne.getSubmitTime();

    Application appTwoDraft =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    assertThat(
            repo.getApplication(appTwoDraft.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);

    // Submit another application for the same program and applicant.
    repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();

    assertThat(
            repo.getApplication(appOne.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(appOne.getSubmitTime()).isEqualTo(initialSubmitTime);

    assertThat(
            repo.getApplication(appTwoDraft.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void submitApplication_doesNotUpdateOtherApplications() {
    Applicant applicant1 = saveApplicant("Alice");
    Applicant applicant2 = saveApplicant("Bob");

    Program program1 = createDraftProgram("Program");
    Program program2 = createDraftProgram("OtherProgram");

    repo.createOrUpdateDraft(applicant1, program1).toCompletableFuture().join();

    Application app2 =
        repo.submitApplication(applicant2, program2, Optional.empty()).toCompletableFuture().join();
    Instant appTwoInitialSubmitTime = app2.getSubmitTime();

    repo.submitApplication(applicant1, program1, Optional.empty()).toCompletableFuture().join();

    assertThat(app2.getSubmitTime()).isEqualTo(appTwoInitialSubmitTime);
    assertThat(app2.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void createOrUpdateDraftApplication_updatesExistingDraft() {
    Applicant applicant = saveApplicant("Alice");
    Program program = createDraftProgram("Program");
    Application appDraft1 =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();
    Application appDraft2 =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    assertThat(appDraft1.id).isEqualTo(appDraft2.id);
    // Since this is a draft application, it shouldn't have a submit time set.
    assertThat(appDraft2.getSubmitTime()).isNull();
  }

  @Test
  public void submitApplication_twoDraftsThrowsException() {
    Applicant applicant = saveApplicant("Alice");
    Program program = createDraftProgram("Program");
    Application appDraft1 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft1.save();
    Application appDraft2 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft2.save();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                repo.submitApplication(applicant, program, Optional.empty())
                    .toCompletableFuture()
                    .join());

    assertThat(exception.getCause().getMessage()).contains("Found more than one DRAFT application");
    assertThat(
            repo.getApplication(appDraft1.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);
    assertThat(
            repo.getApplication(appDraft2.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);
  }

  @Test
  public void submitApplication_noDrafts() {
    Applicant applicant = saveApplicant("Alice");
    Program program = createDraftProgram("Program");
    Application app =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    assertThat(repo.getApplication(app.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  private Application createSubmittedAppAtInstant(Program program, Instant submitTime) {
    // Use a distinct applicant for each application since it's not possible to create multiple
    // submitted applications for the same program for a given applicant.
    Applicant applicant = saveApplicant("Alice");
    Application app =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    app.refresh();
    app.setSubmitTimeForTest(submitTime).save();
    return app;
  }

  @Test
  public void getApplications() {
    Program programOne = createDraftProgram("first");
    Program programTwo = createDraftProgram("second");

    Instant yesterday = dateConverter.parseIso8601DateToStartOfDateInstant("2022-01-02");
    Instant today = dateConverter.parseIso8601DateToStartOfDateInstant("2022-01-03");
    Instant tomorrow = dateConverter.parseIso8601DateToStartOfDateInstant("2022-01-04");

    // Create applications at each instant in each program.
    Application programOneYesterday = createSubmittedAppAtInstant(programOne, yesterday);
    Application programOneToday = createSubmittedAppAtInstant(programOne, today);
    Application programOneTomorrow = createSubmittedAppAtInstant(programOne, tomorrow);
    Application programTwoYesterday = createSubmittedAppAtInstant(programTwo, yesterday);
    Application programTwoToday = createSubmittedAppAtInstant(programTwo, today);
    Application programTwoTomorrow = createSubmittedAppAtInstant(programTwo, tomorrow);

    // No filters. Includes all.
    assertThat(repo.getApplications(TimeFilter.builder().build()).stream().map(a -> a.id))
        .containsExactly(
            programOneYesterday.id,
            programOneToday.id,
            programOneTomorrow.id,
            programTwoYesterday.id,
            programTwoToday.id,
            programTwoTomorrow.id);

    // Only from.
    TimeFilter fromFilter = TimeFilter.builder().setFromTime(Optional.of(today)).build();
    assertThat(repo.getApplications(fromFilter).stream().map(a -> a.id))
        .containsExactly(
            programOneToday.id, programOneTomorrow.id, programTwoToday.id, programTwoTomorrow.id);

    // Only to.
    TimeFilter toFilter = TimeFilter.builder().setToTime(Optional.of(today)).build();
    assertThat(repo.getApplications(toFilter).stream().map(a -> a.id))
        .containsExactly(programOneYesterday.id, programTwoYesterday.id);

    // Both.
    TimeFilter bothFilter =
        TimeFilter.builder()
            .setFromTime(Optional.of(today))
            .setToTime(Optional.of(tomorrow))
            .build();
    assertThat(repo.getApplications(bothFilter).stream().map(a -> a.id))
        .containsExactly(programOneToday.id, programTwoToday.id);
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().putString(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }

  private Program createDraftProgram(String name) {
    Program program =
        new Program(name, "desc", name, "desc", "", DisplayMode.PUBLIC.getValue(), draftVersion);
    program.save();
    return program;
  }
}
