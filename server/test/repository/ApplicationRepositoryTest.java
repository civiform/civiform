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
import org.junit.Before;
import org.junit.Test;
import services.Path;

public class ApplicationRepositoryTest extends ResetPostgres {
  private ApplicationRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(ApplicationRepository.class);
  }

  @Test
  public void submitApplication_updatesOtherApplicationVersions() {
    Applicant one = saveApplicant("Alice");
    Program pOne = saveProgram("Program");

    Application appOne =
        repo.submitApplication(one, pOne, Optional.empty()).toCompletableFuture().join();
    Instant initialSubmitTime = appOne.getSubmitTime();

    Application appTwoDraft = repo.createOrUpdateDraft(one, pOne).toCompletableFuture().join();

    assertThat(repo.getApplication(appOne.id).toCompletableFuture().join()).contains(appOne);
    assertThat(repo.getApplication(appTwoDraft.id).toCompletableFuture().join())
        .contains(appTwoDraft);
    assertThat(
            repo.getApplication(appTwoDraft.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);

    // Submit another application that matches appOne.
    repo.submitApplication(one, pOne, Optional.empty()).toCompletableFuture().join();

    // Ensure that the old one is now "obsolete".
    assertThat(
            repo.getApplication(appOne.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.OBSOLETE);
    // Ensure the submit time didn't get changed when the application was set as obsolete.
    assertThat(appOne.getSubmitTime()).isEqualTo(initialSubmitTime);
    // And that the DRAFT is now ACTIVE.
    assertThat(
            repo.getApplication(appTwoDraft.id)
                .toCompletableFuture()
                .join()
                .get()
                .getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void submitApplication_doesNotUpdateOtherPrograms() {
    Applicant one = saveApplicant("Alice");
    Applicant two = saveApplicant("Bob");

    Program pOne = saveProgram("Program");
    Program pTwo = saveProgram("OtherProgram");

    Application appOne = repo.createOrUpdateDraft(one, pOne).toCompletableFuture().join();

    Application appTwo =
        repo.submitApplication(two, pTwo, Optional.empty()).toCompletableFuture().join();

    Instant appTwoInitialSubmitTime = appTwo.getSubmitTime();

    assertThat(repo.getApplication(appOne.id).toCompletableFuture().join()).contains(appOne);
    assertThat(repo.getApplication(appTwo.id).toCompletableFuture().join()).contains(appTwo);

    // Submit application for different program.
    repo.submitApplication(one, pOne, Optional.empty()).toCompletableFuture().join();

    // Ensure the submit time didn't get changed when the application was set as obsolete.
    assertThat(appTwo.getSubmitTime()).isEqualTo(appTwoInitialSubmitTime);
  }

  @Test
  public void createOrUpdateDraftApplication_updatesExistingDraft() {
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");
    Application application =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();
    Application applicationTwo =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    assertThat(application.id).isEqualTo(applicationTwo.id);
    // Since this is a draft application, it shouldn't have a submit time set.
    assertThat(applicationTwo.getSubmitTime()).isNull();
  }

  @Test
  public void submitApplication_twoDraftsSubmitLatest() {
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");
    Application appOne = Application.create(applicant, program, LifecycleStage.DRAFT);
    Application appTwo = Application.create(applicant, program, LifecycleStage.DRAFT);
    appOne.save();
    appTwo.save();

    assertThat(repo.getApplication(appOne.id).toCompletableFuture().join()).contains(appOne);
    assertThat(repo.getApplication(appTwo.id).toCompletableFuture().join()).contains(appTwo);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                repo.submitApplication(applicant, program, Optional.empty())
                    .toCompletableFuture()
                    .join());

    assertThat(exception.getCause().getMessage()).contains("Found more than one DRAFT application");
    assertThat(
            repo.getApplication(appOne.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);
    assertThat(
            repo.getApplication(appTwo.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);
  }

  @Test
  public void submitApplication_noDrafts() {
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");
    Application app =
        repo.submitApplication(applicant, program, Optional.empty()).toCompletableFuture().join();
    assertThat(repo.getApplication(app.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.ACTIVE);
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().putString(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }

  private Program saveProgram(String name) {
    Program program = new Program(name, "desc", name, "desc", "", DisplayMode.PUBLIC.getValue());
    program.save();
    return program;
  }
}
