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
import repository.ResetPostgres;
import repository.UserRepository;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class ProgramAdminManagementControllerTest extends ResetPostgres {

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
  public void update_addOnly_succeeds() {
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
  public void update_addedEmailDoesNotExist_fails() {
    String email1 = "one";
    String email2 = "two";
    Account account1 = new Account();
    account1.setEmailAddress(email1);
    account1.save();

    String programName = "controller test";
    Program program = ProgramBuilder.newDraftProgram(programName).build();
    Http.Request request =
        addCSRFToken(
                fakeRequest()
                    .bodyForm(ImmutableMap.of("adminEmails[0]", email1, "adminEmails[1]", email2)))
            .build();

    Result result = controller.update(request, program.id);
    account1 = userRepository.lookupAccount(email1).get();

    // Assert the update succeeded (for email1)
    assertThat(result.status()).isEqualTo(OK);
    assertThat(account1.getAdministeredProgramNames()).containsOnly(programName);

    // Add account 2
    Account account2 = new Account();
    account2.setEmailAddress(email2);
    account2.save();
    account2 = userRepository.lookupAccount(email2).get();

    // Account 2 should not have any programs because the account was nonexistent when we addded
    // email2 as a program admin.
    assertThat(account2.getAdministeredProgramNames()).isEmpty();
  }

  @Test
  public void update_withRemovals_succeeds() {
    String programName = "add remove";
    Program program = ProgramBuilder.newDraftProgram(programName).build();

    String addEmail = "add me";
    Account addAccount = new Account();
    addAccount.setEmailAddress(addEmail);
    addAccount.save();

    String removeEmail = "remove me";
    Account removeAccount = new Account();
    removeAccount.setEmailAddress(removeEmail);
    removeAccount.addAdministeredProgram(program.getProgramDefinition());
    removeAccount.save();

    Http.Request request =
        fakeRequest()
            .bodyForm(
                ImmutableMap.of("adminEmails[0]", addEmail, "removeAdminEmails[0]", removeEmail))
            .build();

    Result result = controller.update(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(userRepository.lookupAccount(addEmail).get().getAdministeredProgramNames())
        .containsExactly(programName);
    assertThat(userRepository.lookupAccount(removeEmail).get().getAdministeredProgramNames())
        .isEmpty();
  }

  @Test
  public void update_programNotFound_returnsNotFound() {
    Http.Request request =
        fakeRequest().bodyForm(ImmutableMap.of("adminEmails[0]", "unused")).build();
    Result result = controller.update(request, 1234L);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
