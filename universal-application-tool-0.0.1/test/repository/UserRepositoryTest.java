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

public class UserRepositoryTest extends WithPostgresContainer {

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
    String email = "email@email.com";
    Account account = new Account();
    account.setEmailAddress(email);
    account.save();

    String programName = "name";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    Optional<CiviFormError> result = repo.addAdministeredProgram(email, program);

    assertThat(repo.lookupAccount(email).get().getAdministeredProgramNames())
        .containsOnly(programName);
    assertThat(result).isEqualTo(Optional.empty());
  }

  @Test
  public void addAdministeredProgram_missingAccount_returnsError() {
    String email = "test@test.com";
    String programName = "name";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    Optional<CiviFormError> result = repo.addAdministeredProgram(email, program);

    assertThat(repo.lookupAccount(email)).isEqualTo(Optional.empty());
    assertThat(result
        .isEqualTo(
            Optional.of(
              CiviFormError.of(
                String.format(
                    "%s does not have an admin account and cannot be added as a Program Admin.", email
                   ))));
  }

  @Test
  public void addAdministeredProgram_blankEmail_doesNotCreateAccount() {
    String programName = "name";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();
    String blankEmail = "    ";

    repo.addAdministeredProgram(blankEmail, program);

    assertThat(repo.lookupAccount(blankEmail)).isEmpty();
  }

  @Test
  public void removeAdministeredProgram_succeeds() {
    String programName = "program";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    String email = "happy@test.com";
    Account account = new Account();
    account.setEmailAddress(email);
    account.addAdministeredProgram(program);
    account.save();
    assertThat(account.getAdministeredProgramNames()).contains(programName);

    repo.removeAdministeredProgram(email, program);

    assertThat(repo.lookupAccount(email).get().getAdministeredProgramNames())
        .doesNotContain(programName);
  }

  @Test
  public void removeAdministeredProgram_accountNotAdminForProgram_doesNothing() {
    String programName = "program";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    String email = "happy@test.com";
    Account account = new Account();
    account.setEmailAddress(email);
    account.save();
    assertThat(account.getAdministeredProgramNames()).doesNotContain(programName);

    repo.removeAdministeredProgram(email, program);

    assertThat(repo.lookupAccount(email).get().getAdministeredProgramNames())
        .doesNotContain(programName);
  }

  private Applicant saveApplicant(String name) {
    Applicant applicant = new Applicant();
    applicant.getApplicantData().putString(Path.create("$.applicant.name"), name);
    applicant.save();
    return applicant;
  }
}
