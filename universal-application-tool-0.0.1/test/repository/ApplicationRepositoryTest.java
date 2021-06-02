package repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import services.Path;

public class ApplicationRepositoryTest extends WithPostgresContainer {
  private ApplicationRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(ApplicationRepository.class);
  }

  @Test
  public void submitApplication_updatesOtherApplications() {
    Applicant one = saveApplicant("Alice");
    Applicant two = saveApplicant("Bob");

    Program pOne = saveProgram("Program");
    Program pTwo = saveProgram("OtherProgram");

    Application appOne =
        repo.submitApplication(one, pOne, Optional.empty()).toCompletableFuture().join();
    Application appTwo = repo.createOrUpdateDraft(one, pOne).toCompletableFuture().join();
    Application appThree =
        repo.submitApplication(two, pTwo, Optional.empty()).toCompletableFuture().join();

    assertThat(repo.getApplication(appOne.id).toCompletableFuture().join()).contains(appOne);
    assertThat(repo.getApplication(appTwo.id).toCompletableFuture().join()).contains(appTwo);
    assertThat(
            repo.getApplication(appTwo.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.DRAFT);
    assertThat(repo.getApplication(appThree.id).toCompletableFuture().join()).contains(appThree);

    // Submit another application that matches appOne.
    repo.submitApplication(one, pOne, Optional.empty()).toCompletableFuture().join();

    // Ensure that the old one is now "obsolete".
    assertThat(
            repo.getApplication(appOne.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.OBSOLETE);
    // And that the DRAFT is DELETED.
    assertThat(
            repo.getApplication(appTwo.id).toCompletableFuture().join().get().getLifecycleStage())
        .isEqualTo(LifecycleStage.DELETED);
  }

  @Test
  public void createOrUpdateDraftApplication_updatesExistingDraft() {
    Applicant applicant = saveApplicant("Alice");
    Program program = saveProgram("Program");
    Application application =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();
    Instant initialSubmitTime = application.getSubmitTime();
    Application applicationTwo =
        repo.createOrUpdateDraft(applicant, program).toCompletableFuture().join();

    assertThat(application.id).isEqualTo(applicationTwo.id);
    assertThat(applicationTwo.getSubmitTime()).isAfter(initialSubmitTime);
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().putString(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }

  private Program saveProgram(String name) {
    Program program = new Program(name, "desc", name, "desc");
    program.save();
    return program;
  }
}
