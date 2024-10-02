package models;

import static org.assertj.core.api.Assertions.assertThat;

import auth.oidc.IdTokens;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
  public void canAddTiNotes() {
    AccountModel account = new AccountModel();
    account.setTiNote("Ti notes test");
    account.save();
    assertThat(account.getTiNote()).isEqualTo("Ti notes test");
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
  public void manageIdTokens() {
    AccountModel accountToSave = new AccountModel();
    String email = "fake email";
    accountToSave.setEmailAddress(email);

    IdTokens idTokens = new IdTokens(ImmutableMap.of("session1", "token1", "session2", "token2"));
    accountToSave.setIdTokens(idTokens);
    accountToSave.save();

    Optional<AccountModel> restoredAccount = repository.lookupAccountByEmail(email);
    assertThat(restoredAccount).isNotEmpty();

    assertThat(restoredAccount.get().getIdTokens().getIdToken("session1")).hasValue("token1");
    assertThat(restoredAccount.get().getIdTokens().getIdToken("session2")).hasValue("token2");
  }

  private Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);

  @Test
  public void getActiveSession() {
    AccountModel accountModel = new AccountModel();
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    accountModel.addActiveSession("session1", clock);
    accountModel.storeIdTokenInActiveSession("session1", "idToken1");

    SessionDetails sessionDetails = accountModel.getActiveSession("session1").get();

    assertThat(sessionDetails.getCreationTime()).isEqualTo(Instant.ofEpochSecond(10));
    assertThat(sessionDetails.getIdToken()).isEqualTo("idToken1");
  }

  @Test
  public void addActiveSession() {
    AccountModel accountModel = new AccountModel();
    Clock clock = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);
    accountModel.addActiveSession("session1", clock);

    SessionDetails sessionDetails = accountModel.getActiveSession("session1").get();

    assertThat(sessionDetails.getCreationTime()).isEqualTo(Instant.ofEpochSecond(100));
  }

  @Test
  public void storeIdTokenInActiveSession() {
    AccountModel accountModel = new AccountModel();
    accountModel.addActiveSession("session1", CLOCK);
    accountModel.storeIdTokenInActiveSession("session1", "idToken1");

    SessionDetails sessionDetails = accountModel.getActiveSession("session1").get();

    assertThat(sessionDetails.getIdToken()).isEqualTo("idToken1");
  }

  @Test
  public void removeActiveSession() {
    AccountModel accountModel = new AccountModel();
    accountModel.addActiveSession("session1", CLOCK);
    accountModel.addActiveSession("session2", CLOCK);

    accountModel.removeActiveSession("session1");

    assertThat(accountModel.getActiveSession("session1")).isEmpty();
    assertThat(accountModel.getActiveSession("session2")).isPresent();
  }

  @Test
  public void removeExpiredActiveSessions() {
    AccountModel accountModel = new AccountModel();
    Clock clock90 = Clock.fixed(Instant.ofEpochSecond(90), ZoneOffset.UTC);
    Clock clock100 = Clock.fixed(Instant.ofEpochSecond(100), ZoneOffset.UTC);
    Clock clock110 = Clock.fixed(Instant.ofEpochSecond(110), ZoneOffset.UTC);
    accountModel.addActiveSession("session1", clock90);
    accountModel.addActiveSession("session2", clock100);
    accountModel.addActiveSession("session3", clock110);

    accountModel.removeExpiredActiveSessions(clock110, Duration.ofSeconds(10));

    assertThat(accountModel.getActiveSession("session1")).isEmpty();
    assertThat(accountModel.getActiveSession("session2")).isPresent();
    assertThat(accountModel.getActiveSession("session3")).isPresent();
  }
}
