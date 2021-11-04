package services.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.Account;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import repository.UserRepository;
import repository.WithPostgresContainer;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class RoleServiceTest extends WithPostgresContainer {

  private UserRepository userRepository;
  private RoleService service;

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    service = instanceOf(RoleService.class);
  }

  @Test
  public void makeProgramAdmins_allPromoted() throws ProgramNotFoundException {
    String email1 = "fake@email.com";
    String email2 = "fake2.com";
    Account account1 = new Account();
    account1.setEmailAddress(email1);
    account1.save();
    Account account2 = new Account();
    account2.setEmailAddress(email2);
    account2.save();

    String programName = "test program";
    Program program = ProgramBuilder.newDraftProgram(programName).build();

    Optional<CiviFormError> result =
        service.makeProgramAdmins(program.id, ImmutableSet.of(email1, email2));

    assertThat(result).isEmpty();

    account1 = userRepository.lookupAccount(email1).get();
    account2 = userRepository.lookupAccount(email2).get();

    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(account2.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void makeProgramAdmins_emptyList_returnsEmptyOptional() throws ProgramNotFoundException {
    assertThat(service.makeProgramAdmins(1L, ImmutableSet.of())).isEmpty();
  }

  @Test
  public void makeProgramAdmins_listOfBlankEmails_returnsEmptyOptional()
      throws ProgramNotFoundException {
    assertThat(service.makeProgramAdmins(1L, ImmutableSet.of(" ", "", "    "))).isEmpty();
  }

  @Test
  public void makeProgramAdmins_programNotFound_throwsException() {
    assertThatThrownBy(() -> service.makeProgramAdmins(1234L, ImmutableSet.of("email@email.com")))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void removeProgramAdmins_succeeds() throws ProgramNotFoundException {
    String programName = "to remove";
    ProgramDefinition toRemove = ProgramBuilder.newDraftProgram(programName).buildDefinition();
    String extraName = "extra";
    ProgramDefinition extra = ProgramBuilder.newDraftProgram(extraName).buildDefinition();

    Account one = new Account();
    String emailOne = "one@test.com";
    one.setEmailAddress(emailOne);
    one.addAdministeredProgram(toRemove);
    one.save();

    Account two = new Account();
    String emailTwo = "two@test.com";
    two.setEmailAddress(emailTwo);
    two.addAdministeredProgram(toRemove);
    two.addAdministeredProgram(extra);
    two.save();

    assertThat(one.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(two.getAdministeredProgramNames()).containsOnly(programName, extraName);

    service.removeProgramAdmins(toRemove.id(), ImmutableSet.of(emailOne, emailTwo));

    assertThat(userRepository.lookupAccount(emailOne).get().getAdministeredProgramNames())
        .isEmpty();
    assertThat(userRepository.lookupAccount(emailTwo).get().getAdministeredProgramNames())
        .containsOnly(extraName);
  }

  @Test
  public void removeProgramAdmins_noProgram_throwsProgramNotFoundException() {
    assertThatThrownBy(() -> service.removeProgramAdmins(1234L, ImmutableSet.of("test")))
        .isInstanceOf(ProgramNotFoundException.class);
  }
}
