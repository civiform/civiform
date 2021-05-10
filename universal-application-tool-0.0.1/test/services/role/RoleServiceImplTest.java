package services.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import models.Account;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import repository.UserRepository;
import repository.WithPostgresContainer;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class RoleServiceImplTest extends WithPostgresContainer {

  private UserRepository userRepository;
  private RoleService service;

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    service = instanceOf(RoleServiceImpl.class);
  }

  @Test
  public void makeProgramAdmins_allPromoted() throws ProgramNotFoundException {
    String email1 = "fake@email.com";
    String email2 = "fake2@email.com";
    String programName = "test program";
    Program program = ProgramBuilder.newDraftProgram(programName).build();

    service.makeProgramAdmins(program.id, ImmutableSet.of(email1, email2));

    Account account1 = userRepository.lookupAccount(email1).get();
    Account account2 = userRepository.lookupAccount(email2).get();

    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(account2.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void makeProgramAdmins_programNotFound_throwsException() {
    assertThatThrownBy(() -> service.makeProgramAdmins(1234L, ImmutableSet.of()))
        .isInstanceOf(ProgramNotFoundException.class);
  }
}
