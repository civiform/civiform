package models;

import static org.assertj.core.api.Assertions.assertThat;

import auth.oidc.SerializedIdTokens;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
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

  @Test
  public void manageSerializedIdTokens() {
    AccountModel accountToSave = new AccountModel();
    String email = "fake email";
    accountToSave.setEmailAddress(email);

    SerializedIdTokens serializedIdTokens =
        new SerializedIdTokens(ImmutableMap.of("session1", "token1", "session2", "token2"));
    accountToSave.setSerializedIdTokens(serializedIdTokens);
    accountToSave.save();

    Optional<AccountModel> restoredAccount = repository.lookupAccountByEmail(email);
    assertThat(restoredAccount).isNotEmpty();

    assertThat(restoredAccount.get().getSerializedIdTokens().size()).isEqualTo(2);
    assertThat(restoredAccount.get().getSerializedIdTokens().get("session1")).isEqualTo("token1");
    assertThat(restoredAccount.get().getSerializedIdTokens().get("session2")).isEqualTo("token2");
  }
}
