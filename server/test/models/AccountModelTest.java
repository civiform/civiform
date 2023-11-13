package models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class AccountModelTest extends ResetPostgres {

  private AccountRepository repository;

  @Before
  public void setup() {
    repository = instanceOf(AccountRepository.class);
  }

  @Test
  public void canAddAdministeredProgram() {
    AccountModel account = new AccountModel();
    String email = "fake email";
    account.setEmailAddress(email);

    ProgramDefinition one = ProgramBuilder.newDraftProgram("one").build().getProgramDefinition();
    ProgramDefinition two = ProgramBuilder.newDraftProgram("two").build().getProgramDefinition();
    account.addAdministeredProgram(one);
    account.addAdministeredProgram(two);

    account.save();

    AccountModel found = repository.lookupAccountByEmail(email).get();
    assertThat(found.getAdministeredProgramNames()).containsExactly("one", "two");
  }

  @Test
  public void addDuplicateProgram_doesNotAddToList() {
    AccountModel account = new AccountModel();
    String programName = "duplicate";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    account.addAdministeredProgram(program);
    assertThat(account.getAdministeredProgramNames()).containsOnly(programName);

    // Try to add again.
    account.addAdministeredProgram(program);
    assertThat(account.getAdministeredProgramNames()).containsExactly(programName);
  }

  @Test
  public void removeAdministeredProgram() {
    AccountModel account = new AccountModel();
    String programName = "remove";
    ProgramDefinition program = ProgramBuilder.newDraftProgram(programName).buildDefinition();

    account.addAdministeredProgram(program);
    assertThat(account.getAdministeredProgramNames()).containsExactly(programName);

    account.removeAdministeredProgram(program);
    assertThat(account.getAdministeredProgramNames()).isEmpty();
  }
}
