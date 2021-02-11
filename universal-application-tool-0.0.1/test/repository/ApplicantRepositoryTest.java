package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.Applicant;
import org.junit.Test;

public class ApplicantRepositoryTest extends WithPostgresContainer {

  @Test
  public void createApplicant() {
    ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();
    applicant.id = 2L;
    String path = "$.applicant";
    applicant.getApplicantData().put(path, "birthDate", "1/1/2021");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    Applicant a = repo.lookupApplicant(2L).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(2L);
    assertThat(a.getApplicantData().read("$.applicant.birthDate", String.class))
        .isEqualTo("1/1/2021");
  }
}
