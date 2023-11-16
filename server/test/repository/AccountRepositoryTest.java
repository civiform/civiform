package repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import models.Account;
import models.Applicant;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.CiviFormError;
import services.Path;
import services.WellKnownPaths;
import services.program.ProgramDefinition;
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
    Set<Applicant> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).isEmpty();
  }

  @Test
  public void listApplicants() {
    Applicant one = saveApplicant("one");
    Applicant two = saveApplicant("two");

    Set<Applicant> allApplicants = repo.listApplicants().toCompletableFuture().join();

    assertThat(allApplicants).containsExactly(one, two);
  }

  @Test
  public void lookupApplicant_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<Applicant> found = repo.lookupApplicant(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicant_findsCorrectApplicant() {
    saveApplicant("Alice");
    Applicant two = saveApplicant("Bob");

    Optional<Applicant> found = repo.lookupApplicant(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void lookupByAuthorityId() {

    new Account().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    assertThat(repo.lookupAccountByAuthorityId(AUTHORITY_ID).get().getEmailAddress())
        .isEqualTo(EMAIL);
  }

  @Test
  public void lookupByEmailAddress() {
    new Account().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

    assertThat(repo.lookupAccountByEmail(EMAIL).get().getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void lookupByEmailAddressAsync() {
    new Account().setEmailAddress(EMAIL).setAuthorityId(AUTHORITY_ID).save();

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
    Applicant applicant = new Applicant();
    String path = "$." + WellKnownPaths.APPLICANT_DOB.toString();
    applicant.getApplicantData().putDate(Path.create(path), "2021-01-01");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    Applicant a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().getDateOfBirth().get().toString()).isEqualTo("2021-01-01");
  }

  @Test
  public void updateApplicant() {
    Applicant applicant = new Applicant();
    repo.insertApplicant(applicant).toCompletableFuture().join();
    String path = "$." + WellKnownPaths.APPLICANT_DOB.toString();
    applicant.getApplicantData().putString(Path.create(path), "1/1/2021");

    repo.updateApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    Applicant a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().readString(Path.create(path))).hasValue("1/1/2021");
  }

  @Test
  public void lookupApplicantSync_returnsEmptyOptionalWhenApplicantNotFound() {
    Optional<Applicant> found = repo.lookupApplicantSync(1L);

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupApplicantSync_findsCorrectApplicant() {
    saveApplicant("Alice");
    Applicant two = saveApplicantWithDob("Bob", "2022-07-07");

    Optional<Applicant> found = repo.lookupApplicantSync(two.id);

    assertThat(found).hasValue(two);
    assertThat(found.get().getApplicantData().getDateOfBirth().get().toString())
        .isEqualTo("2022-07-07");
  }

  @Test
  public void addAdministeredProgram_existingAccount_succeeds() {
    Account account = new Account();
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

    Account account = new Account();
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

    Account account = new Account();
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

    Applicant newUnusedGuest = resourceCreator.insertApplicantWithAccount();
    Applicant oldUnusedGuest = resourceCreator.insertApplicantWithAccount();
    Applicant oldUsedGuest = resourceCreator.insertApplicantWithAccount();
    resourceCreator.insertApplication(oldUsedGuest, testProgram, LifecycleStage.DRAFT);
    Applicant oldUnusedAuthenticated =
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
  public void findApplicantsWithIncorrectDobPath() {
    // Save an applicant with the correct path for dob
    saveApplicantWithDob("Foo", "2001-11-01");

    // Save an applicant with the incorrect path for dob
    Applicant applicantWithDeprecatedPath = saveApplicant("Bar");
    applicantWithDeprecatedPath
        .getApplicantData()
        .putDate(WellKnownPaths.APPLICANT_DOB_DEPRECATED, "2002-12-02");
    applicantWithDeprecatedPath.save();

    List<Applicant> applicants = repo.findApplicantsWithIncorrectDobPath().findList();
    // Only the applicant with the incorrect path should be returned
    assertThat(applicants.size()).isEqualTo(1);
    assertThat(applicants.get(0).getApplicantData().getApplicantName().get()).isEqualTo("Bar");
  }

  private Applicant saveApplicantWithDob(String name, String dob) {
    Applicant applicant = new Applicant();
    applicant
        .getApplicantData()
        .putString(Path.create("$." + WellKnownPaths.APPLICANT_FIRST_NAME.toString()), name);
    applicant.getApplicantData().setDateOfBirth(dob);
    applicant.save();
    return applicant;
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant
        .getApplicantData()
        .putString(Path.create("$." + WellKnownPaths.APPLICANT_FIRST_NAME.toString()), name);
    applicant.save();
    return applicant;
  }
}
