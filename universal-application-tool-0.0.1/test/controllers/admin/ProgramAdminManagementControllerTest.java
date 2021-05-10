package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.Account;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import repository.WithPostgresContainer;
import support.ProgramBuilder;

public class ProgramAdminManagementControllerTest extends WithPostgresContainer {

  private UserRepository userRepository;
  private ProgramAdminManagementController controller;

  @Before
  public void setup() {
    userRepository = instanceOf(UserRepository.class);
    controller = instanceOf(ProgramAdminManagementController.class);
  }

  @Test
  public void update_succeeds() {
    String programName = "controller test";
    Program program = ProgramBuilder.newDraftProgram(programName).build();
    Http.Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("adminEmails[0]", "one", "adminEmails[1]", "two"))
            .build();

    Result result = controller.update(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    Account account1 = userRepository.lookupAccount("one").get();
    Account account2 = userRepository.lookupAccount("two").get();
    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(account2.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void update_programNotFound_returnsNotFound() {
    Result result = controller.update(fakeRequest().build(), 1234L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
