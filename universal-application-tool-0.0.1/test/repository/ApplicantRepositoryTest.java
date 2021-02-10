package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.Applicant;
import org.junit.Test;

public class ApplicantRepositoryTest extends WithPostgresContainer {

  @Test
  public void createApplicant() {
    ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();
    applicant.id = 1L;
    String path = "$.applicant";
    applicant.getApplicantData().put(path, "birthDate", "1/1/2021");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    Applicant a = repo.lookupApplicant(1L).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(1L);
    assertThat(a.getApplicantData().read("$.applicant.birthDate", String.class))
        .isEqualTo("1/1/2021");
  }
}
