package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableMap;
import models.AccountModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class ProgramAdminManagementControllerTest extends ResetPostgres {

  private AccountRepository accountRepository;
  private ProgramAdminManagementController controller;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    controller = instanceOf(ProgramAdminManagementController.class);
  }

  @Test
  public void edit_rendersForm() {
    ProgramModel program = ProgramBuilder.newDraftProgram("Success").build();

    Result result = controller.edit(addCSRFToken(fakeRequest()).build(), program.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Manage admins for program: Success");
  }

  @Test
  public void edit_rendersFormWithExistingAdmins() {
    ProgramDefinition program = ProgramBuilder.newDraftProgram("Existing admins").buildDefinition();
    AccountModel existingAdmin = resourceCreator.insertAccount();
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
  public void add_succeeds() {
    String programName = "add test";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    String email = "add me";
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    account.save();

    Http.Request request = fakeRequest().bodyForm(ImmutableMap.of("adminEmail", email)).build();

    Result result = controller.add(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    account = accountRepository.lookupAccountByEmail(email).get();
    assertThat(account.getAdministeredProgramNames()).containsOnly(programName);
  }

  @Test
  public void delete_succeeds() {
    String programName = "delete test";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    String deleteEmail = "delete me";
    AccountModel deleteAccount = new AccountModel();
    deleteAccount.setEmailAddress(deleteEmail);
    deleteAccount.addAdministeredProgram(program.getProgramDefinition());
    deleteAccount.save();
    assertThat(
            accountRepository.lookupAccountByEmail(deleteEmail).get().getAdministeredProgramNames())
        .isNotEmpty();

    Http.Request request =
        fakeRequest().bodyForm(ImmutableMap.of("adminEmail", deleteEmail)).build();
    Result result = controller.delete(request, program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(
            accountRepository.lookupAccountByEmail(deleteEmail).get().getAdministeredProgramNames())
        .isEmpty();
  }

  @Test
  public void delete_nonExistentEmail_doesNotReturnError() {
    String programName = "delete test";
    ProgramModel program = ProgramBuilder.newDraftProgram(programName).build();

    String adminEmail = "admin";
    AccountModel adminAccount = new AccountModel();
    adminAccount.setEmailAddress(adminEmail);
    adminAccount.addAdministeredProgram(program.getProgramDefinition());
    adminAccount.save();
    assertThat(
            accountRepository.lookupAccountByEmail(adminEmail).get().getAdministeredProgramNames())
        .isNotEmpty();

    Http.Request request =
        fakeRequest().bodyForm(ImmutableMap.of("adminEmail", "nonExistentEmail")).build();
    Result result = controller.delete(request, program.id);

    // The controller doesn't return an error in this case.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }
}
