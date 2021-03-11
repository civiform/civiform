package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicantRepository;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;

public class ApplicantTest extends WithPostgresContainer {

  private ApplicantRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(ApplicantRepository.class);
  }

  @Test
  public void hasAnApplicantDataWhenCreated() {
    Applicant applicant = new Applicant();
    assertThat(applicant.getApplicantData()).isInstanceOf(ApplicantData.class);
  }

  @Test
  public void persistsChangesToTheApplicantData() throws Exception {
    Applicant applicant = new Applicant();

    Path path = Path.create("$.applicant.birthDate");
    applicant.getApplicantData().putString(path, "1/1/2021");
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().readString(path)).hasValue("1/1/2021");
  }

  @Test
  public void storesAndRetrievesPreferredLocale() {
    // Default to English
    Applicant applicant = new Applicant();
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.ENGLISH);

    // Set locale
    applicant.getApplicantData().setPreferredLocale(Locale.FRANCE);
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.FRANCE);
  }

  @Test
  public void createsOnlyOneApplicantData() {
    Applicant applicant = new Applicant();

    ApplicantData applicantData = applicant.getApplicantData();

    assertThat(applicant.getApplicantData()).isEqualTo(applicantData);
  }
}
