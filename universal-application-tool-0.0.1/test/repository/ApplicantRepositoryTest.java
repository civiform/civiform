package repository;

import static org.assertj.core.api.Assertions.assertThat;

import models.Applicant;
import org.junit.Test;

public class ApplicantRepositoryTest extends WithPostgresContainer {

  @Test
  public void createApplicant() {
    ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();
    String path = "$.applicant";
    applicant.getApplicantData().put(path, "birthDate", "1/1/2021");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    Applicant a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().read("$.applicant.birthDate", String.class))
        .isEqualTo("1/1/2021");
  }
}
