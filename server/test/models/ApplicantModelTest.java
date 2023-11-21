package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;

public class ApplicantModelTest extends ResetPostgres {

  private AccountRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(AccountRepository.class);
  }

  @Test
  public void hasAnApplicantDataWhenCreated() {
    ApplicantModel applicant = new ApplicantModel();
    assertThat(applicant.getApplicantData()).isInstanceOf(ApplicantData.class);
  }

  @Test
  public void persistsChangesToTheApplicantData() throws Exception {
    ApplicantModel applicant = new ApplicantModel();

    Path path = Path.create("$.applicant.birthDate");
    applicant.getApplicantData().putString(path, "1/1/2021");
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().readString(path)).hasValue("1/1/2021");
  }

  @Test
  public void storesAndRetrievesPreferredLocale() {
    // Default to English
    ApplicantModel applicant = new ApplicantModel();
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.US);

    // Set locale
    applicant.getApplicantData().setPreferredLocale(Locale.FRANCE);
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.FRANCE);
  }

  @Test
  public void missingPreferredLocale_doesNotSetLocaleOnApplicantData() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
  }

  @Test
  public void createsOnlyOneApplicantData() {
    ApplicantModel applicant = new ApplicantModel();

    ApplicantData applicantData = applicant.getApplicantData();

    assertThat(applicant.getApplicantData()).isEqualTo(applicantData);
  }

  @Test
  public void mergeApplicantData() {
    ApplicantData data1 = new ApplicantModel().getApplicantData();
    Path foo = Path.create("$.applicant.foo");
    Path subMapFoo = Path.create("$.applicant.subObject.foo");
    Path subMapBar = Path.create("$.applicant.subObject.bar");
    data1.putString(foo, "foo");
    data1.putString(subMapFoo, "also_foo");
    ApplicantData data2 = new ApplicantModel().getApplicantData();
    data2.putString(foo, "bar");
    data2.putString(subMapBar, "bar");
    data1.putString(subMapFoo, "also_foo");

    List<Path> removedPaths = data1.mergeFrom(data2);

    assertThat(removedPaths).contains(foo);
    assertThat(removedPaths).doesNotContain(subMapFoo);
    assertThat(data1.readString(subMapBar)).isNotEmpty();
  }
}
