package repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import models.Account;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.CiviFormError;
import services.Path;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class UserRepositoryTest extends ResetPostgres {
  public static final String EMAIL = "email@email.com";
  public static final String PROGRAM_NAME = "program";
  public static final String AUTHORITY_ID = "I'm an authority ID";

  private UserRepository repo;

  @Before
  public void setupApplicantRepository() {
    repo = instanceOf(UserRepository.class);
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
  public void insertApplicant() {
    Applicant applicant = new Applicant();
    String path = "$.applicant.birthdate";
    applicant.getApplicantData().putString(Path.create(path), "1/1/2021");

    repo.insertApplicant(applicant).toCompletableFuture().join();

    long id = applicant.id;
    Applicant a = repo.lookupApplicant(id).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(id);
    assertThat(a.getApplicantData().readString(Path.create(path))).hasValue("1/1/2021");
  }

  @Test
  public void updateApplicant() {
    Applicant applicant = new Applicant();
    repo.insertApplicant(applicant).toCompletableFuture().join();
    String path = "$.applicant.birthdate";
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
    Applicant two = saveApplicant("Bob");

    Optional<Applicant> found = repo.lookupApplicantSync(two.id);

    assertThat(found).hasValue(two);
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
                        "%s does not have an admin account and cannot be added as a Program Admin.",
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

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().putString(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }
}
