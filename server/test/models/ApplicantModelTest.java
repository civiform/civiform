package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    Path foo = Path.create("$.applicant.foo");
    // Children in the 'subObject' json map.
    Path subObjectFoo = Path.create("$.applicant.subObject.foo");
    Path subObjectBar = Path.create("$.applicant.subObject.bar");
    // A list of selections.
    Path listColor = Path.create("$.applicant.colors");

    // The main data.
    ApplicantData data1 = new ApplicantModel().getApplicantData();
    data1.putString(foo, "foo");
    data1.putString(subObjectFoo, "also_foo");
    data1.putArray(listColor, Lists.newArrayList("red"));

    // The 'new' data that will be merged from.
    ApplicantData data2 = new ApplicantModel().getApplicantData();
    // Different value for existing key, won't be copied.
    data2.putString(foo, "bar");
    // Same value as existing value in existing map, will be ignored.
    data2.putString(subObjectFoo, "also_foo");
    // New value in new key, will be copied.
    data2.putString(subObjectBar, "bar");
    // List values will be added to the existing ones, even duplicates.
    data2.putArray(listColor, Lists.newArrayList("red", "green"));

    List<Path> removedPaths = data1.mergeFrom(data2);

    // The new value was different so it was dropped.
    assertThat(removedPaths).contains(foo);
    // The new value was the same so it's not considered removed/dropped
    assertThat(removedPaths).doesNotContain(subObjectFoo);
    // Assert the expected values are present.
    assertThat(data1.readString(foo)).hasValue("foo");
    assertThat(data1.readString(subObjectBar)).hasValue("bar");
    assertThat(data1.readStringList(listColor).orElse(ImmutableList.of()))
        .containsExactly("red", "red", "green");
  }

  @Test
  public void savesPrimaryApplicantInfoColumns() {
    String firstName = "firstName";
    String middleName = "middleName";
    String lastName = "lastName";
    String suffix = "suffix";
    String emailAddress = "email@address.com";
    String countryCode = "US";
    String phoneNumber = "1234567890";
    LocalDate dob = LocalDate.now(ZoneId.systemDefault());
    ApplicantModel applicant = new ApplicantModel();
    applicant.setFirstName(firstName);
    applicant.setMiddleName(middleName);
    applicant.setLastName(lastName);
    applicant.setSuffix(suffix);
    applicant.setEmailAddress(emailAddress);
    applicant.setCountryCode(countryCode);
    applicant.setPhoneNumber(phoneNumber);
    applicant.setDateOfBirth(dob);
    applicant.save();
    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    assertThat(applicant.getFirstName().get()).isEqualTo(firstName);
    assertThat(applicant.getMiddleName().get()).isEqualTo(middleName);
    assertThat(applicant.getLastName().get()).isEqualTo(lastName);
    assertThat(applicant.getSuffix().get()).isEqualTo(suffix);
    assertThat(applicant.getEmailAddress().get()).isEqualTo(emailAddress);
    assertThat(applicant.getCountryCode().get()).isEqualTo(countryCode);
    assertThat(applicant.getPhoneNumber().get()).isEqualTo(phoneNumber);
    assertThat(applicant.getDateOfBirth().get()).isEqualTo(dob);
  }

  @Test
  public void savesPrimaryApplicantInfoColumns_withEmptyNameSuffix() {
    String firstName = "firstName";
    String middleName = "middleName";
    String lastName = "lastName";
    String suffix = "";
    String emailAddress = "email@address.com";
    String countryCode = "US";
    String phoneNumber = "1234567890";
    LocalDate dob = LocalDate.now(ZoneId.systemDefault());
    ApplicantModel applicant = new ApplicantModel();
    applicant.setFirstName(firstName);
    applicant.setMiddleName(middleName);
    applicant.setLastName(lastName);
    applicant.setSuffix(suffix);
    applicant.setEmailAddress(emailAddress);
    applicant.setCountryCode(countryCode);
    applicant.setPhoneNumber(phoneNumber);
    applicant.setDateOfBirth(dob);
    applicant.save();
    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    assertThat(applicant.getFirstName().get()).isEqualTo(firstName);
    assertThat(applicant.getMiddleName().get()).isEqualTo(middleName);
    assertThat(applicant.getLastName().get()).isEqualTo(lastName);
    assertThat(applicant.getSuffix()).isEmpty();
    assertThat(applicant.getEmailAddress().get()).isEqualTo(emailAddress);
    assertThat(applicant.getCountryCode().get()).isEqualTo(countryCode);
    assertThat(applicant.getPhoneNumber().get()).isEqualTo(phoneNumber);
    assertThat(applicant.getDateOfBirth().get()).isEqualTo(dob);
  }

  @Test
  public void savesPrimaryApplicantInfoColumns_withNullNameSuffix() {
    String firstName = "firstName";
    String middleName = "middleName";
    String lastName = "lastName";
    String emailAddress = "email@address.com";
    String countryCode = "US";
    String phoneNumber = "1234567890";
    LocalDate dob = LocalDate.now(ZoneId.systemDefault());
    ApplicantModel applicant = new ApplicantModel();
    applicant.setFirstName(firstName);
    applicant.setMiddleName(middleName);
    applicant.setLastName(lastName);
    applicant.setEmailAddress(emailAddress);
    applicant.setCountryCode(countryCode);
    applicant.setPhoneNumber(phoneNumber);
    applicant.setDateOfBirth(dob);
    applicant.save();
    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    assertThat(applicant.getFirstName().get()).isEqualTo(firstName);
    assertThat(applicant.getMiddleName().get()).isEqualTo(middleName);
    assertThat(applicant.getLastName().get()).isEqualTo(lastName);
    assertThat(applicant.getSuffix()).isEmpty();
    assertThat(applicant.getEmailAddress().get()).isEqualTo(emailAddress);
    assertThat(applicant.getCountryCode().get()).isEqualTo(countryCode);
    assertThat(applicant.getPhoneNumber().get()).isEqualTo(phoneNumber);
    assertThat(applicant.getDateOfBirth().get()).isEqualTo(dob);
  }

  @Test
  public void savesPhoneNumberWithCorrectFormat() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setPhoneNumber("(503) 823-4000");
    applicant.save();
    applicant = repo.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    assertThat(applicant.getPhoneNumber().get()).isEqualTo("5038234000");
    assertThat(applicant.getCountryCode().get()).isEqualTo("US");
  }

  @Test
  public void getApplicantName_exists() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setUserName("First Last");
    assertThat(applicant.getApplicantName()).isEqualTo(Optional.of("Last, First"));
    assertThat(applicant.getFirstName()).isEqualTo(Optional.of("First"));
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName()).isEqualTo(Optional.of("Last"));
    assertThat(applicant.getSuffix()).isEmpty();
  }

  @Test
  public void getApplicantName_withMiddleNameWithoutSuffix_exists() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setUserName("First Middle Last");
    assertThat(applicant.getApplicantName()).isEqualTo(Optional.of("Last, First"));
    assertThat(applicant.getFirstName()).isEqualTo(Optional.of("First"));
    assertThat(applicant.getMiddleName().get()).isEqualTo("Middle");
    assertThat(applicant.getLastName()).isEqualTo(Optional.of("Last"));
    assertThat(applicant.getSuffix()).isEmpty();
  }

  @Test
  public void getApplicantName_withNameSuffixWithoutMiddleName_exist() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setUserName("First Last Jr.");
    assertThat(applicant.getApplicantName()).isEqualTo(Optional.of("Last, First"));
    assertThat(applicant.getFirstName()).isEqualTo(Optional.of("First"));
    assertThat(applicant.getMiddleName()).isEmpty();
    assertThat(applicant.getLastName()).isEqualTo(Optional.of("Last"));
    assertThat(applicant.getSuffix().get()).isEqualTo("Jr.");
  }

  @Test
  public void getApplicantName_withAllNameFields_exist() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setUserName("First Middle Last Jr.");
    assertThat(applicant.getApplicantName()).isEqualTo(Optional.of("Last, First"));
    assertThat(applicant.getFirstName()).isEqualTo(Optional.of("First"));
    assertThat(applicant.getMiddleName().get()).isEqualTo("Middle");
    assertThat(applicant.getLastName()).isEqualTo(Optional.of("Last"));
    assertThat(applicant.getSuffix().get()).isEqualTo("Jr.");
  }

  @Test
  public void getApplicantName_noName() {
    ApplicantModel applicant = new ApplicantModel();
    assertThat(applicant.getApplicantName()).isEmpty();
  }

  @Test
  public void getApplicantDisplayName() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.setUserName("First Middle Last Jr.");
    assertThat(applicant.getApplicantDisplayName()).isEqualTo(Optional.of("Last, First"));
  }

  @Test
  public void getApplicantDisplayName_fallsBackToEmail() {
    ApplicantModel applicant = new ApplicantModel();
    AccountModel account = new AccountModel();
    account.setEmailAddress("myemail@email.com");
    account.save();
    applicant.setAccount(account);
    applicant.save();

    assertThat(applicant.getApplicantDisplayName()).isEqualTo(Optional.of("myemail@email.com"));
  }

  @Test
  public void getApplicantDisplayName_empty() {
    ApplicantModel applicant = new ApplicantModel();
    AccountModel account = new AccountModel();
    account.save();
    applicant.setAccount(account);
    applicant.save();
    assertThat(applicant.getApplicantDisplayName()).isEmpty();
  }
}
