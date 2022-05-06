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
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");

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

    Program program1 = saveProgram("Program");
    Program program2 = saveProgram("OtherProgram");

    repo.createOrUpdateDraft(applicant1, program1).toCompletableFuture().join();

    Application app2 =
            repo.submitApplication(applicant2, program2, Optional.empty()).toCompletableFuture().join();
    Instant appTwoInitialSubmitTime = app2.getSubmitTime();

    repo.submitApplication(applicant1, program1, Optional.empty()).toCompletableFuture().join();

    assertThat(app2.getSubmitTime()).isEqualTo(appTwoInitialSubmitTime);
  }

  @Test
  public void createOrUpdateDraftApplication_updatesExistingDraft() {
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");
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
    Program program = saveProgram("Program");
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
