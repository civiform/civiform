package models;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.UserRepository;
import services.Path;
import services.applicant.ApplicantData;

public class ApplicantTest extends ResetPostgres {

  private UserRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(UserRepository.class);
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
    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.US);

    // Set locale
    applicant.getApplicantData().setPreferredLocale(Locale.FRANCE);
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().preferredLocale()).isEqualTo(Locale.FRANCE);
  }

  @Test
  public void missingPreferredLocale_doesNotSetLocaleOnApplicantData() {
    Applicant applicant = new Applicant();
    applicant.save();

    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();

    assertThat(applicant.getApplicantData().hasPreferredLocale()).isFalse();
  }

  @Test
  public void createsOnlyOneApplicantData() {
    Applicant applicant = new Applicant();

    ApplicantData applicantData = applicant.getApplicantData();

    assertThat(applicant.getApplicantData()).isEqualTo(applicantData);
  }

  @Test
  public void mergeApplicantData() {
    ApplicantData data1 = new Applicant().getApplicantData();
    Path foo = Path.create("$.applicant.foo");
    Path subMapFoo = Path.create("$.applicant.subObject.foo");
    Path subMapBar = Path.create("$.applicant.subObject.bar");
    data1.putString(foo, "foo");
    data1.putString(subMapFoo, "also_foo");
    ApplicantData data2 = new Applicant().getApplicantData();
    data2.putString(foo, "bar");
    data2.putString(subMapBar, "bar");
    data1.putString(subMapFoo, "also_foo");

    List<Path> removedPaths = data1.mergeFrom(data2);

    assertThat(removedPaths).contains(foo);
    assertThat(removedPaths).doesNotContain(subMapFoo);
    assertThat(data1.readString(subMapBar)).isNotEmpty();
  }
}
