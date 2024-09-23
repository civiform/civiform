package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import models.AccountModel;
import models.ApplicantModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import services.CiviFormError;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.program.ProgramDefinition;
import services.ti.EmailAddressExistsException;
import support.ProgramBuilder;

public class AccountRepositoryTest extends ResetPostgres {
  public static final String EMAIL = "email@email.com";
  public static final String PROGRAM_NAME = "program";
  public static final String AUTHORITY_ID = "I'm an authority ID";

  private AccountRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(AccountRepository.class);
  }

  @Test
  public void listApplicants_empty() {
    Set<ApplicantModel> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).isEmpty();
  }

  @Test
  public void updateClientEmail_ThrowsEmailExistsException() {
    ApplicantModel applicantModel = setupApplicantForUpdateTest();
    setupAccountForUpdateTest();
    AccountModel account = new AccountModel();
    account.setEmailAddress("test1@test.com");
    account.save();
    assertThatThrownBy(
            () ->
                repo.updateTiClient(
                    account,
                    applicantModel,
                    "first",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "test@test.com",
                    "2020-10-10"))
        .isInstanceOf(EmailAddressExistsException.class);
  }

  @Test
  public void updateClientNameTest_AllNonEmpty() {
    ApplicantModel applicantUpdateTest = setupApplicantForUpdateTest();
    AccountModel account = setupAccountForUpdateTest();
    repo.updateTiClient(
        account, applicantUpdateTest, "Dow", "James", "John", "Jr.", "", "", "", "2020-10-10");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantFirstName().get())
        .isEqualTo("Dow");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantMiddleName().get())
        .isEqualTo("James");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantLastName().get())
        .isEqualTo("John");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantNameSuffix().get())
        .isEqualTo("Jr.");
  }

  @Test
  public void updateClientNameTest_EmptyMiddleLastNameAndSuffix() {
    ApplicantModel applicantUpdateTest = setupApplicantForUpdateTest();
    AccountModel account = setupAccountForUpdateTest();
    repo.updateTiClient(account, applicantUpdateTest, "John", "", "", "", "", "", "", "2020-10-10");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantFirstName().get())
        .isEqualTo("John");
    assertThat(applicantUpdateTest.getApplicantData().getApplicantMiddleName()).isEmpty();
    assertThat(applicantUpdateTest.getApplicantData().getApplicantLastName()).isEmpty();
    assertThat(applicantUpdateTest.getApplicantData().getApplicantNameSuffix()).isEmpty();
  }

  @Test
  public void updateDobTest() {
    ApplicantModel applicantUpdateTest = setupApplicantForUpdateTest();
    AccountModel account = setupAccountForUpdateTest();
    repo.updateTiClient(
        account, applicantUpdateTest, "Dow", "James", "John", "Jr.", "", "", "", "2023-12-12");
    assertThat(applicantUpdateTest.getApplicantData().getDateOfBirth().get())
        .isEqualTo("2023-12-12");
  }

  @Test
  public void updatePhoneNumberTest() {
    ApplicantModel applicantUpdateTest = setupApplicantForUpdateTest();
    AccountModel account = setupAccountForUpdateTest();
    repo.updateTiClient(
        account,
        applicantUpdateTest,
        "Dow",
        "James",
        "John",
        "Jr.",
        "4259746144",
        "",
        "",
        "2023-12-12");
    assertThat(applicantUpdateTest.getApplicantData().getPhoneNumber().get())
        .isEqualTo("4259746144");
  }

  @Test
  public void updateTiNoteTest() {
    ApplicantModel applicantUpdateTest = setupApplicantForUpdateTest();
    AccountModel account = setupAccountForUpdateTest();
    repo.updateTiClient(
        account,
        applicantUpdateTest,
        "Dow",
        "James",
        "John",
        "Jr.",
        "",
        "this is notes",
        "",
        "2023-12-12");
    assertThat(account.getTiNote()).isEqualTo("this is notes");
  }

  @Test
  public void listApplicants() {
    ApplicantModel one = saveApplicant("one");
    ApplicantModel two = saveApplicant("two");

    Set<ApplicantModel> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).containsExactly(one, two);
  }

  @Test
  public void lookupApplicant_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<ApplicantModel> found = repo.lookupApplicant(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicant_findsCorrectApplicant() {
    saveApplicant("Alice");
    ApplicantModel two = saveApplicant("Bob");

    Optional<ApplicantModel> found = repo.lookupApplicant(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void lookupByAuthorityId() {

    new AccountModel().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    assertThat(repo.lookupAccountByAuthorityId(AUTHORITY_ID).get().getEmailAddress())
        .isEqualTo(EMAIL);
  }

  @Test
  public void lookupByEmailAddress() {
    new AccountModel().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    assertThat(repo.lookupAccountByEmail(EMAIL).get().getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void lookupByEmailAddressAsync() {
    new AccountModel().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    assertThat(
            repo.lookupAccountByEmailAsync(EMAIL)
                .toCompletableFuture()
                .join()
                .get()
                .getAuthorityId())
        .isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void insertApplicant() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.getApplicantData().setDateOfBirth("2021-01-01");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    ApplicantModel a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().getDateOfBirth().get().toString()).isEqualTo("2021-01-01");
  }

  @Test
  public void updateApplicant() {
    ApplicantModel applicant = new ApplicantModel();
    repo.insertApplicant(applicant).toCompletableFuture().join();
    applicant.getApplicantData().setDateOfBirth("2021-01-01");

    repo.updateApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    ApplicantModel a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().getDateOfBirth().get().toString()).isEqualTo("2021-01-01");
  }

  @Test
  public void lookupApplicantSync_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<ApplicantModel> found = repo.lookupApplicantSync(1L);

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicantSync_findsCorrectApplicant() {
    saveApplicant("Alice");
    ApplicantModel two = saveApplicantWithDob("Bob", "2022-07-07");

    Optional<ApplicantModel> found = repo.lookupApplicantSync(two.id);

    assertThat(found).hasValue(two);
    assertThat(found.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
  }

  @Test
  public void addAdministeredProgram_existingAccount_succeeds() {
    AccountModel account = new AccountModel();
    account.setEmailAddress(EMAIL);
    account.save();

    ProgramDefinition program = ProgramBuilder.newDraftProgram(PROGRAM_NAME).buildDefinition();

    Optional<CiviFormError> result = repo.addAdministeredProgram(EMAIL, program);

    assertThat(repo.lookupAccountByEmail(EMAIL).get().getAdministeredProgramNames())
        .containsOnly(PROGRAM_NAME);
    assertThat(result).isEqualTo(Optional.empty());
  }

  @Test
  public void addAdministeredProgram_missingAccount_returnsError() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram(PROGRAM_NAME).buildDefinition();

    Optional<CiviFormError> result = repo.addAdministeredProgram(EMAIL, program);

    assertThat(repo.lookupAccountByEmail(EMAIL)).isEqualTo(Optional.empty());
    assertThat(result)
        .isEqualTo(
            Optional.of(
                CiviFormError.of(
                    String.format(
                        "Cannot add %s as a Program Admin because they do not have an admin"
                            + " account. Have the user log in as admin on the home page, then they"
                            + " can be added as a Program Admin.",
                        EMAIL))));
  }

  @Test
  public void addAdministeredProgram_blankEmail_doesNotCreateAccount() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram(PROGRAM_NAME).buildDefinition();
    String blankEmail = "    ";

    repo.addAdministeredProgram(blankEmail, program);

    assertThat(repo.lookupAccountByEmail(blankEmail)).isEmpty();
  }

  @Test
  public void removeAdministeredProgram_succeeds() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram(PROGRAM_NAME).buildDefinition();

    AccountModel account = new AccountModel();
    account.setEmailAddress(EMAIL);
    account.addAdministeredProgram(program);
    account.save();
    assertThat(account.getAdministeredProgramNames()).contains(PROGRAM_NAME);

    repo.removeAdministeredProgram(EMAIL, program);

    assertThat(repo.lookupAccountByEmail(EMAIL).get().getAdministeredProgramNames())
        .doesNotContain(PROGRAM_NAME);
  }

  @Test
  public void removeAdministeredProgram_accountNotAdminForProgram_doesNothing() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram(PROGRAM_NAME).buildDefinition();

    AccountModel account = new AccountModel();
    account.setEmailAddress(EMAIL);
    account.save();
    assertThat(account.getAdministeredProgramNames()).doesNotContain(PROGRAM_NAME);

    repo.removeAdministeredProgram(EMAIL, program);

    assertThat(repo.lookupAccountByEmail(EMAIL).get().getAdministeredProgramNames())
        .doesNotContain(PROGRAM_NAME);
  }

  @Test
  public void deleteUnusedGuestAccounts() {
    var testProgram = resourceCreator.insertActiveProgram("test-program");
    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    Instant timeInPast = now.minus(10, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC);

    ApplicantModel newUnusedGuest = resourceCreator.insertApplicantWithAccount();
    ApplicantModel oldUnusedGuest = resourceCreator.insertApplicantWithAccount();
    ApplicantModel oldUsedGuest = resourceCreator.insertApplicantWithAccount();
    resourceCreator.insertApplication(oldUsedGuest, testProgram, LifecycleStage.DRAFT);
    ApplicantModel oldUnusedAuthenticated =
        resourceCreator.insertApplicantWithAccount(Optional.of("registered-user@example.com"));

    oldUnusedGuest.setWhenCreated(timeInPast).save();
    oldUsedGuest.setWhenCreated(timeInPast).save();
    oldUnusedAuthenticated.setWhenCreated(timeInPast).save();

    var numberDeleted = repo.deleteUnusedGuestAccounts(5);
    var remainingApplicants = repo.listApplicants().toCompletableFuture().join();
    var remainingAccounts = repo.listAccounts();

    assertThat(remainingApplicants).contains(newUnusedGuest);
    assertThat(remainingAccounts).contains(newUnusedGuest.getAccount());

    assertThat(remainingApplicants).contains(oldUsedGuest);
    assertThat(remainingAccounts).contains(oldUsedGuest.getAccount());

    assertThat(remainingApplicants).contains(oldUnusedAuthenticated);
    assertThat(remainingAccounts).contains(oldUnusedAuthenticated.getAccount());

    assertThat(remainingApplicants).doesNotContain(oldUnusedGuest);
    assertThat(remainingAccounts).doesNotContain(oldUnusedGuest.getAccount());

    assertThat(numberDeleted).isEqualTo(1);
    assertThat(remainingApplicants).hasSize(3);
  }

  @Test
  public void findApplicantsNeedingPrimaryApplicantInfoDataMigration() {
    // First name only
    ApplicantModel applicantFirstName = savePlainApplicant();
    applicantFirstName.getApplicantData().putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicantFirstName.save();

    // DOB only (using preseeded question)
    ApplicantModel applicantDob = savePlainApplicant();
    applicantDob.getApplicantData().putString(WellKnownPaths.APPLICANT_DOB, "1999-01-11");
    applicantDob.save();

    // All the things a TI client has (except optional email address)
    ApplicantModel applicantTiClient = savePlainApplicant();
    ApplicantData applicantData = applicantTiClient.getApplicantData();
    applicantData.putString(WellKnownPaths.APPLICANT_FIRST_NAME, "Jean");
    applicantData.putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, "Luc");
    applicantData.putString(WellKnownPaths.APPLICANT_LAST_NAME, "Picard");
    applicantData.putString(WellKnownPaths.APPLICANT_PHONE_NUMBER, "5038234000");
    applicantData.putDate(WellKnownPaths.APPLICANT_DOB, "2305-07-13");
    applicantTiClient.save();

    // Applicant with an account email address and no primary applicant info email
    ApplicantModel applicantAccountEmail = savePlainApplicant();
    AccountModel account = applicantAccountEmail.getAccount();
    account.setEmailAddress("picard@starfleet.com");
    account.save();

    // Applicant with both an account email and primary applicant info email
    ApplicantModel applicantPaiEmail = savePlainApplicant();
    applicantPaiEmail.setEmailAddress("picard@starfleet.com");
    AccountModel accountPaiEmail = applicantPaiEmail.getAccount();
    accountPaiEmail.setEmailAddress("enterprise@starfleet.com");
    accountPaiEmail.save();
    applicantPaiEmail.save();

    // Applicant with no well known path data or account email address
    ApplicantModel applicantNoData = savePlainApplicant();

    List<ApplicantModel> applicants =
        repo.findApplicantsNeedingPrimaryApplicantInfoDataMigration().findList();
    assertThat(applicants)
        .containsOnly(applicantFirstName, applicantDob, applicantTiClient, applicantAccountEmail);
    assertThat(applicants).doesNotContain(applicantNoData);
    assertThat(applicants).doesNotContain(applicantPaiEmail);
  }

  @Test
  public void updateSerializedIdTokens() {
    AccountModel account = new AccountModel();
    String fakeEmail = "fake email";
    account.setEmailAddress(fakeEmail);
    account.save();
    long accountId = account.id;

    // Create a JWT that just expired.
    LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    Instant timeInPast = now.minus(1, ChronoUnit.SECONDS).toInstant(ZoneOffset.UTC);
    JWT expiredJwt = getJwtWithExpirationTime(timeInPast);

    repo.updateSerializedIdTokens(account, "sessionId1", expiredJwt.serialize());

    // Create a JWT that won't expire for an hour.
    Instant timeInFuture = now.plus(1, ChronoUnit.HOURS).toInstant(ZoneOffset.UTC);
    JWT validJwt = getJwtWithExpirationTime(timeInFuture);

    repo.updateSerializedIdTokens(account, "sessionId2", validJwt.serialize());

    Optional<AccountModel> retrievedAccount = repo.lookupAccount(accountId);
    assertThat(retrievedAccount).isNotEmpty();
    // Expired token
    assertThat(retrievedAccount.get().getSerializedIdTokens().getIdToken("sessionId1")).isEmpty();
    // Valid token
    assertThat(retrievedAccount.get().getSerializedIdTokens().getIdToken("sessionId2"))
        .hasValue(validJwt.serialize());
  }

  @Test
  public void listTrustedIntermediaryGroups_test() {
    String dummyDesc = "something";
    repo.createNewTrustedIntermediaryGroup("bbc", dummyDesc);
    repo.createNewTrustedIntermediaryGroup("abc", dummyDesc);
    repo.createNewTrustedIntermediaryGroup("zbc", dummyDesc);
    repo.createNewTrustedIntermediaryGroup("cbc", dummyDesc);

    List<TrustedIntermediaryGroupModel> tiGroups = repo.listTrustedIntermediaryGroups();

    assertThat(tiGroups).hasSize(4);
    assertThat(tiGroups.get(0).getName()).isEqualTo("abc");
    assertThat(tiGroups.get(1).getName()).isEqualTo("bbc");
    assertThat(tiGroups.get(2).getName()).isEqualTo("cbc");
    assertThat(tiGroups.get(3).getName()).isEqualTo("zbc");
  }

  private JWT getJwtWithExpirationTime(Instant expirationTime) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder().expirationTime(Date.from(expirationTime)).build();
    return new PlainJWT(claims);
  }

  private ApplicantModel saveApplicantWithDob(String name, String dob) {
    ApplicantModel applicant = new ApplicantModel();
    AccountModel account = new AccountModel().setEmailAddress(String.format("%s@email.com", name));
    account.save();
    applicant.setAccount(account);
    applicant
        .getApplicantData()
        .setUserName(name, Optional.empty(), Optional.empty(), Optional.empty());
    applicant.getApplicantData().setDateOfBirth(dob);
    applicant.save();
    return applicant;
  }

  private ApplicantModel saveApplicant(String name) {
    ApplicantModel applicant = new ApplicantModel();
    AccountModel account = new AccountModel().setEmailAddress(String.format("%s@email.com", name));
    account.save();
    applicant.setAccount(account);
    applicant
        .getApplicantData()
        .setUserName(name, Optional.empty(), Optional.empty(), Optional.empty());
    applicant.save();
    return applicant;
  }

  private ApplicantModel savePlainApplicant() {
    ApplicantModel applicant = new ApplicantModel();
    AccountModel account = new AccountModel();
    account.save();
    applicant.setAccount(account);
    applicant.save();
    return applicant;
  }

  private ApplicantModel setupApplicantForUpdateTest() {
    ApplicantModel applicantUpdateTest = new ApplicantModel();
    ApplicantData applicantDateUpdateTest = applicantUpdateTest.getApplicantData();
    applicantDateUpdateTest.setUserName(
        "Jane", Optional.empty(), Optional.of("Doe"), Optional.empty());
    applicantDateUpdateTest.setDateOfBirth("2022-10-10");
    applicantUpdateTest.save();
    return applicantUpdateTest;
  }

  private AccountModel setupAccountForUpdateTest() {
    AccountModel accountUpdateTest = new AccountModel();
    accountUpdateTest.setEmailAddress("test@test.com");
    accountUpdateTest.save();
    return accountUpdateTest;
  }
}
