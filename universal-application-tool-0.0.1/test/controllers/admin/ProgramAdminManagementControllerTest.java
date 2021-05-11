package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
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
import services.program.ProgramDefinition;
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
  public void edit_rendersForm() {
    Program program = ProgramBuilder.newDraftProgram("Success").build();

    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Manage Admins for Program: Success");
  }

  @Test
  public void edit_rendersFormWithExistingAdmins() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram("Existing Admins").buildDefinition();
    Account existingAdmin = resourceCreator.insertAccount();
    existingAdmin.setEmailAddress("test@test.com");
    existingAdmin.addAdministeredProgram(program);
    existingAdmin.save();

    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), program.id());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("test@test.com");
  }

  @Test
  public void edit_programNotFound() {
    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), 1234L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void update_succeeds() {
    String email1 = "one";
    String email2 = "two";
    Account account1 = new Account();
    Account account2 = new Account();
    account1.setEmailAddress(email1);
    account2.setEmailAddress(email2);
    account1.save();
    account2.save();

    String programName = "controller test";
    Program program = ProgramBuilder.newDraftProgram(programName).build();
    Http.Request request =
        fakeRequest()
            .bodyForm(ImmutableMap.of("adminEmails[0]", "one", "adminEmails[1]", "two"))
            .build();

    Result result = controller.update(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    account1 = userRepository.lookupAccount("one").get();
    account2 = userRepository.lookupAccount("two").get();
    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);
    assertThat(account2.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void update_programNotFound_returnsNotFound() {
    Result result = controller.update(fakeRequest().build(), 1234L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
