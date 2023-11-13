package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import models.AccountModel;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.Path;
import services.applicant.exception.DuplicateApplicationException;
import services.program.ProgramType;
import support.CfTestHelpers;

public class ApplicationRepositoryTest extends ResetPostgres {
  private ApplicationRepository repo;
  private DateConverter dateConverter;

  private Version draftVersion;
  private Version activeVersion;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationRepository.class);
    dateConverter = instanceOf(DateConverter.class);
    draftVersion = new Version(LifecycleStage.DRAFT);
    draftVersion.save();
    activeVersion = new Version(LifecycleStage.ACTIVE);
    activeVersion.save();
  }

  @Test
  public void submitApplication_updatesOtherApplicationVersions() {
    Applicant applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");

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

    // Submit another application for the same program and applicant, but update the applicantData
    // object so it is not detected as a duplicate.
    applicant.getApplicantData().putString(Path.create("text"), "text");
    repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();

    assertThat(
            repo.getApplication(appOne.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(appOne.getSubmitTime()).isEqualTo(initialSubmitTime);

    Application appTwoSubmitted =
        repo.getApplication(appTwoDraft.id).toCompletableFuture().join().get();
    assertThat(appTwoSubmitted.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(appTwoSubmitted.getApplicantData().getApplicantName().get()).isEqualTo("Alice");
    assertThat(repo.getApplication(appOne.id).toCompletableFuture().join().get().getSubmitTime())
        .isEqualTo(initialSubmitTime);
  }

  @Test
  public void submitApplication_doesNotUpdateOtherProgramApplications() {
    Applicant applicant1 = saveApplicant("Alice");
    Applicant applicant2 = saveApplicant("Bob");

    ProgramModel program1 = createDraftProgram("Program");
    ProgramModel program2 = createDraftProgram("OtherProgram");

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
    ProgramModel program = createActiveProgram("Program");
    Application appDraft1 =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    // If the applicant already has an application to a different version of
    // the same program, that version should be used.
    ProgramModel programV2 = createDraftProgram("Program");

    assertThat(program.id).isNotEqualTo(programV2.id);

    Application appDraft2 =
        repo.createOrUpdateDraft(applicant, programV2).toCompletableFuture().join();

    assertThat(appDraft1.id).isEqualTo(appDraft2.id);
    // Since this is a draft application, it shouldn't have a submit time set.
    assertThat(appDraft2.getSubmitTime()).isNull();
    // Applicant data is not saved in the application until it is submitted.
    assertThat(appDraft1.getApplicantData().getApplicantName()).isEmpty();
    assertThat(appDraft2.getApplicantData().getApplicantName()).isEmpty();
  }

  @Test
  public void submitApplication_twoDraftsThrowsException() {
    Applicant applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");
    Application appDraft1 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft1.save();
    Application appDraft2 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft2.save();

    assertThatThrownBy(
            () ->
                repo.submitApplication(applicant, program, Optional.empty())
                    .toCompletableFuture()
                    .join())
        .isInstanceOf(RuntimeException.class)
        .cause()
        .hasMessageContaining("Found more than one DRAFT application");

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
    ProgramModel program = createDraftProgram("Program");
    Application app =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    assertThat(repo.getApplication(app.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void submitApplication_duplicateSubmissionsThrowsException() {
    Applicant applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");

    repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    assertThatThrownBy(
            () ->
                repo.submitApplication(applicant, program, Optional.empty())
                    .toCompletableFuture()
                    .join())
        .cause()
        .isInstanceOf(DuplicateApplicationException.class);
  }

  private Application createSubmittedAppAtInstant(ProgramModel program, Instant submitTime) {
    // Use a distinct applicant for each application since it's not possible to create multiple
    // submitted applications for the same program for a given applicant.
    Applicant applicant = saveApplicant("Alice");
    Application app = repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();
    CfTestHelpers.withMockedInstantNow(
        submitTime.toString(),
        () -> {
          app.setLifecycleStage(LifecycleStage.ACTIVE);
          app.setSubmitTimeToNow();
          app.save();
        });
    app.refresh();
    return app;
  }

  @Test
  public void getApplications() {
    ProgramModel programOne = createDraftProgram("first");
    ProgramModel programTwo = createDraftProgram("second");

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
    assertThat(repo.getApplications(TimeFilter.EMPTY).stream().map(a -> a.id))
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
    TimeFilter toFilter = TimeFilter.builder().setUntilTime(Optional.of(today)).build();
    assertThat(repo.getApplications(toFilter).stream().map(a -> a.id))
        .containsExactly(programOneYesterday.id, programTwoYesterday.id);

    // Both.
    TimeFilter bothFilter =
        TimeFilter.builder()
            .setFromTime(Optional.of(today))
            .setUntilTime(Optional.of(tomorrow))
            .build();
    assertThat(repo.getApplications(bothFilter).stream().map(a -> a.id))
        .containsExactly(programOneToday.id, programTwoToday.id);

    // Overly restrictive, no apps.
    TimeFilter restrictiveFilter =
        TimeFilter.builder().setFromTime(Optional.of(Instant.now())).build();
    assertThat(repo.getApplications(restrictiveFilter)).isEmpty();
  }

  @Test
  public void getApplicationsForApplicant() throws Exception {
    Applicant applicant = saveApplicant("Applicant");

    ProgramModel program = createDraftProgram("Program");
    Application appDraft1 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft1.save();
    Application appDraft2 = Application.create(applicant, program, LifecycleStage.DRAFT);
    appDraft2.save();
    Application appActive1 = Application.create(applicant, program, LifecycleStage.ACTIVE);
    appActive1.save();
    Application appActive2 = Application.create(applicant, program, LifecycleStage.ACTIVE);
    appActive2.save();
    Application appObsolete1 = Application.create(applicant, program, LifecycleStage.OBSOLETE);
    appObsolete1.save();
    Application appObsolete2 = Application.create(applicant, program, LifecycleStage.OBSOLETE);
    appObsolete2.save();

    ImmutableSet<Application> result =
        repo.getApplicationsForApplicant(
                applicant.id,
                ImmutableSet.of(
                    LifecycleStage.DRAFT, LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
            .toCompletableFuture()
            .get();
    assertThat(result.stream().map(a -> a.id))
        .containsExactlyInAnyOrder(
            appDraft1.id,
            appDraft2.id,
            appActive1.id,
            appActive2.id,
            appObsolete1.id,
            appObsolete2.id);

    result =
        repo.getApplicationsForApplicant(applicant.id, ImmutableSet.of(LifecycleStage.DRAFT))
            .toCompletableFuture()
            .get();
    assertThat(result.stream().map(a -> a.id))
        .containsExactlyInAnyOrder(appDraft1.id, appDraft2.id);

    result =
        repo.getApplicationsForApplicant(applicant.id, ImmutableSet.of())
            .toCompletableFuture()
            .get();
    assertThat(result).isEmpty();
  }

  @Test
  public void getApplicationsForApplicant_filtersById() throws Exception {
    Applicant primaryApplicant = saveApplicant("Applicant");
    Applicant otherApplicant = saveApplicant("Other");

    ProgramModel program = createDraftProgram("Program");
    Application primaryApplicantDraftApp =
        Application.create(primaryApplicant, program, LifecycleStage.DRAFT);
    primaryApplicantDraftApp.save();
    Application otherApplicantActiveApp =
        Application.create(otherApplicant, program, LifecycleStage.ACTIVE);
    otherApplicantActiveApp.save();

    ImmutableSet<Application> result =
        repo.getApplicationsForApplicant(
                otherApplicant.id,
                ImmutableSet.of(
                    LifecycleStage.DRAFT, LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
            .toCompletableFuture()
            .get();
    assertThat(result.stream().map(a -> a.id))
        .containsExactlyInAnyOrder(otherApplicantActiveApp.id);

    result =
        repo.getApplicationsForApplicant(
                primaryApplicant.id,
                ImmutableSet.of(
                    LifecycleStage.DRAFT, LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
            .toCompletableFuture()
            .get();
    assertThat(result.stream().map(a -> a.id))
        .containsExactlyInAnyOrder(primaryApplicantDraftApp.id);

    // Unknown applicant.
    result =
        repo.getApplicationsForApplicant(
                Integer.MAX_VALUE,
                ImmutableSet.of(
                    LifecycleStage.DRAFT, LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
            .toCompletableFuture()
            .get();
    assertThat(result).isEmpty();
  }

  private Applicant saveApplicant(String name) {
    AccountModel account = new AccountModel();
    Applicant applicant = new Applicant();
    applicant.getApplicantData().setUserName(name);
    applicant.setAccount(account);
    applicant.save();
    return applicant;
  }

  private ProgramModel createDraftProgram(String name) {
    return createProgram(name, draftVersion);
  }

  private ProgramModel createActiveProgram(String name) {
    return createProgram(name, activeVersion);
  }

  private ProgramModel createProgram(String name, Version version) {
    ProgramModel program =
        new ProgramModel(
            name,
            "desc",
            name,
            "desc",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            version,
            ProgramType.DEFAULT,
            new ProgramAcls());
    program.save();
    return program;
  }
}
