package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.fakeRequest;

import models.Applicant;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import support.ProgramBuilder;

public class FileControllerTest extends WithMockedProfiles {

  private FileController controller;

  @Before
  public void setUp() {
    resetDatabase();
    controller = instanceOf(FileController.class);
  }

  @Test
  public void show_differentApplicant_returnsUnauthorizedResult() {
    Applicant applicant = createApplicantWithMockedProfile();
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, applicant.id + 1, "fake-file-key").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void show_TIManagedApplicant_redirects() {
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, managedApplicant.id, "fake-file-key").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void show_redirects() {
    Applicant applicant = createApplicantWithMockedProfile();
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, applicant.id, "fake-file-key").toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_invalidProgram_returnsNotFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id + 1, "fake-file-key");
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_differentProgram_returnsUnauthorizedResult() {
    Program programOne = ProgramBuilder.newDraftProgram("one").build();
    Program programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, programTwo.id, "fake-file-key");
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_globalAdmin_returnsUnauthorizedResult() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createGlobalAdminWithMockedProfile();
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, "fake-file-key");
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_globalAdminWhenNoProgramAdmin_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, "fake-file-key");
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, "fake-file-key");
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }
}
