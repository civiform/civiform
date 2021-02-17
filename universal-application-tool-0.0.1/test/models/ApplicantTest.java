package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import repository.ApplicantRepository;
import repository.WithTruncatingTables;
import services.applicant.ApplicantData;

public class ApplicantTest extends WithTruncatingTables {

  @Test
  public void hasAnApplicantDataWhenCreated() {
    Applicant applicant = new Applicant();
    assertThat(applicant.getApplicantData()).isInstanceOf(ApplicantData.class);
  }

  @Test
  public void persistsChangesToTheApplicantData() {
    ApplicantRepository repo = app.injector().instanceOf(ApplicantRepository.class);
    Applicant applicant = new Applicant();

    String path = "$.applicant";
    applicant.getApplicantData().put(path, "birthDate", "1/1/2021");
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().read("$.applicant.birthDate", String.class))
        .isEqualTo("1/1/2021");
  }

  @Test
  public void createsOnlyOneApplicantData() {
    Applicant applicant = new Applicant();

    ApplicantData applicantData = applicant.getApplicantData();

    assertThat(applicant.getApplicantData() == applicantData);
  }
}
