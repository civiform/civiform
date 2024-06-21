package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.VersionModel;
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

  private VersionModel draftVersion;
  private VersionModel activeVersion;

  @Before
  public void setUp() {
    repo = instanceOf(ApplicationRepository.class);
    dateConverter = instanceOf(DateConverter.class);
    draftVersion = new VersionModel(LifecycleStage.DRAFT);
    draftVersion.save();
    activeVersion = new VersionModel(LifecycleStage.ACTIVE);
    activeVersion.save();
  }

  @Test
  public void submitApplication_updatesOtherApplicationVersions() {
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");

    ApplicationModel appOne =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    Instant initialSubmitTime = appOne.getSubmitTime();

    ApplicationModel appTwoDraft =
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

    ApplicationModel appTwoSubmitted =
        repo.getApplication(appTwoDraft.id).toCompletableFuture().join().get();
    assertThat(appTwoSubmitted.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(appTwoSubmitted.getApplicantData().getApplicantName().get()).isEqualTo("Alice");
    assertThat(
            repo.getApplication(appOne.id)
                .toCompletableFuture()
                .join()
                .get()
                .getSubmitTime()
                .truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(initialSubmitTime.truncatedTo(ChronoUnit.SECONDS));
  }

  @Test
  public void submitApplication_doesNotUpdateOtherProgramApplications() {
    ApplicantModel applicant1 = saveApplicant("Alice");
    ApplicantModel applicant2 = saveApplicant("Bob");

    ProgramModel program1 = createDraftProgram("Program");
    ProgramModel program2 = createDraftProgram("OtherProgram");

    repo.createOrUpdateDraft(applicant1, program1).toCompletableFuture().join();

    ApplicationModel app2 =
        repo.submitApplication(applicant2, program2, Optional.empty()).toCompletableFuture().join();
    Instant appTwoInitialSubmitTime = app2.getSubmitTime();

    repo.submitApplication(applicant1, program1, Optional.empty()).toCompletableFuture().join();

    assertThat(app2.getSubmitTime()).isEqualTo(appTwoInitialSubmitTime);
    assertThat(app2.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void createOrUpdateDraftApplication_updatesExistingDraft() {
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel program = createActiveProgram("Program");
    ApplicationModel appDraft1 =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    // If the applicant already has an application to a different version of
    // the same program, that version should be used.
    ProgramModel programV2 = createDraftProgram("Program");

    assertThat(program.id).isNotEqualTo(programV2.id);

    ApplicationModel appDraft2 =
        repo.createOrUpdateDraft(applicant, programV2).toCompletableFuture().join();

    assertThat(appDraft1.id).isEqualTo(appDraft2.id);
    // Since this is a draft application, it shouldn't have a submit time set.
    assertThat(appDraft2.getSubmitTime()).isNull();
    // Applicant data is not saved in the application until it is submitted.
    assertThat(appDraft1.getApplicantData().asJsonString()).isEqualTo("{}");
    assertThat(appDraft2.getApplicantData().asJsonString()).isEqualTo("{}");
  }

  @Test
  public void submitApplication_twoDraftsThrowsException() {
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");
    ApplicationModel appDraft1 = ApplicationModel.create(applicant, program, LifecycleStage.DRAFT);
    appDraft1.save();
    ApplicationModel appDraft2 = ApplicationModel.create(applicant, program, LifecycleStage.DRAFT);
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
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel program = createDraftProgram("Program");
    ApplicationModel app =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    assertThat(repo.getApplication(app.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void submitApplication_duplicateSubmissionsThrowsException() {
    ApplicantModel applicant = saveApplicant("Alice");
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

  private ApplicationModel createSubmittedAppAtInstant(
      ProgramModel program, Instant submitTime, String applicantName) {
    // Use a distinct applicant for each application since it's not possible to create multiple
    // submitted applications for the same program for a given applicant.
    ApplicantModel applicant = saveApplicant(applicantName);
    ApplicationModel app =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();
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

    Instant yesterday = dateConverter.parseIso8601DateToStartOfLocalDateInstant("2022-01-02");
    Instant today = dateConverter.parseIso8601DateToStartOfLocalDateInstant("2022-01-03");
    Instant tomorrow = dateConverter.parseIso8601DateToStartOfLocalDateInstant("2022-01-04");

    // Create applications at each instant in each program.
    ApplicationModel programOneYesterday = createSubmittedAppAtInstant(programOne, yesterday, "a");
    ApplicationModel programOneToday = createSubmittedAppAtInstant(programOne, today, "b");
    ApplicationModel programOneTomorrow = createSubmittedAppAtInstant(programOne, tomorrow, "c");
    ApplicationModel programTwoYesterday = createSubmittedAppAtInstant(programTwo, yesterday, "d");
    ApplicationModel programTwoToday = createSubmittedAppAtInstant(programTwo, today, "e");
    ApplicationModel programTwoTomorrow = createSubmittedAppAtInstant(programTwo, tomorrow, "f");

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
    ApplicantModel applicant = saveApplicant("Applicant");

    ProgramModel program = createDraftProgram("Program");
    ApplicationModel appDraft1 = ApplicationModel.create(applicant, program, LifecycleStage.DRAFT);
    appDraft1.save();
    ApplicationModel appDraft2 = ApplicationModel.create(applicant, program, LifecycleStage.DRAFT);
    appDraft2.save();
    ApplicationModel appActive1 =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE);
    appActive1.save();
    ApplicationModel appActive2 =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE);
    appActive2.save();
    ApplicationModel appObsolete1 =
        ApplicationModel.create(applicant, program, LifecycleStage.OBSOLETE);
    appObsolete1.save();
    ApplicationModel appObsolete2 =
        ApplicationModel.create(applicant, program, LifecycleStage.OBSOLETE);
    appObsolete2.save();

    ImmutableSet<ApplicationModel> result =
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
    ApplicantModel primaryApplicant = saveApplicant("Applicant");
    ApplicantModel otherApplicant = saveApplicant("Other");

    ProgramModel program = createDraftProgram("Program");
    ApplicationModel primaryApplicantDraftApp =
        ApplicationModel.create(primaryApplicant, program, LifecycleStage.DRAFT);
    primaryApplicantDraftApp.save();
    ApplicationModel otherApplicantActiveApp =
        ApplicationModel.create(otherApplicant, program, LifecycleStage.ACTIVE);
    otherApplicantActiveApp.save();

    ImmutableSet<ApplicationModel> result =
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

  @Test
  public void updateDraftApplicationProgram_updatesExistingDraft() {
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel programV1 = createActiveProgram("Program");

    // Check the application points to program v1
    ApplicationModel application =
        repo.createOrUpdateDraft(applicant, programV1).toCompletableFuture().join();
    assertThat(application.getProgram().id).isEqualTo(programV1.id);

    // Update the application program v2
    ProgramModel programV2 = createActiveProgram("Program");
    repo.updateDraftApplicationProgram(applicant.id, programV2.id);

    // Reload the application and check that it points to program v2
    application = repo.getApplication(applicant.id).toCompletableFuture().join().get();
    assertThat(application.getProgram().id).isEqualTo(programV2.id);
  }

  @Test
  public void updateDraftApplicationProgram_throwIfProgramNotFound() {
    ApplicantModel applicant = saveApplicant("Alice");
    ProgramModel programV1 = createActiveProgram("Program");

    // Check the application points to program v1
    ApplicationModel application =
        repo.createOrUpdateDraft(applicant, programV1).toCompletableFuture().join();
    assertThat(application.getProgram().id).isEqualTo(programV1.id);

    // Update the application program v2
    long fakeProgramV2Id = -1;
    assertThatThrownBy(() -> repo.updateDraftApplicationProgram(applicant.id, fakeProgramV2Id))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Program not found");
  }

  private ApplicantModel saveApplicant(String name) {
    AccountModel account = new AccountModel();
    // TODO (#5503): This can be removed when we are no longer checking name
    // against the account email in ApplicantData#getApplicantName
    account.setEmailAddress(String.format("%s@email.com", name.toLowerCase(Locale.ROOT)));
    account.save();

    ApplicantModel applicant = new ApplicantModel();
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

  private ProgramModel createProgram(String name, VersionModel version) {
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
            /* eligibilityIsGating= */ true,
            new ProgramAcls());
    program.save();
    return program;
  }
}
