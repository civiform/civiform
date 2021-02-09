package models;

import static org.assertj.core.api.Assertions.assertThat;

import repository.ApplicantRepository;
import services.applicant.ApplicantData;
import org.junit.Test;
import repository.WithPostgresContainer;

public class ApplicantTest extends WithPostgresContainer {

  @Test
  public void hasAJsonDocumentContextWhenCreated() {
    Applicant applicant = new Applicant();
    assertThat(applicant.getApplicantData()).isInstanceOf(ApplicantData.class);
  }

  @Test
  public void persistsChangesToTheDocumentContext() {
    ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();

    String path = "$.applicant";
    applicant.getApplicantData().put(path, "birthDate", "1/1/2021");
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().read("$.applicant.birthDate", String.class))
        .isEqualTo("Alice");
  }
}
