package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.fakeRequest;

import models.Applicant;
import models.Program;
import models.StoredFile;
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
    StoredFile file = createFakeFile(applicant);
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, applicant.id + 1, file.getName()).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void show_differentFileKey_returnsNotFound() {
    Applicant fileOnwer = createApplicant();
    Applicant applicant = createApplicantWithMockedProfile();
    StoredFile file = createFakeFile(fileOnwer);
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, applicant.id + 1, file.getName()).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void show_TIManagedApplicant_redirects() {
    Applicant managedApplicant = createApplicant();
    StoredFile file = createFakeFile(managedApplicant);
    createTIWithMockedProfile(managedApplicant);
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, managedApplicant.id, file.getName()).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void show_redirects() {
    Applicant applicant = createApplicantWithMockedProfile();
    StoredFile file = createFakeFile(applicant);
    Request request = fakeRequest().build();
    Result result =
        controller.show(request, applicant.id, file.getName()).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_invalidProgram_returnsNotFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    StoredFile file = createFakeFile(program.id);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id + 1, file.getName());
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_differentProgram_returnsUnauthorizedResult() {
    Program programOne = ProgramBuilder.newDraftProgram("one").build();
    Program programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    StoredFile file = createFakeFile(programOne.id);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, programTwo.id, file.getName());
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_globalAdmin_returnsUnauthorizedResult() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createGlobalAdminWithMockedProfile();
    StoredFile file = createFakeFile(program.id);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, file.getName());
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_differentProgram_returnsNotFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    StoredFile file = createFakeFile(9999L);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, file.getName());
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_globalAdminWhenNoProgramAdmin_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    StoredFile file = createFakeFile(program.id);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, file.getName());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    StoredFile file = createFakeFile(program.id);
    Request request = fakeRequest().build();
    Result result = controller.adminShow(request, program.id, file.getName());
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  private StoredFile createFakeFile(Applicant applicant) {
    StoredFile file = new StoredFile(applicant);
    file.setName("fake-file-key");
    file.save();
    return file;
  }

  private StoredFile createFakeFile(long programId) {
    StoredFile file = new StoredFile(null);
    file.setName(String.format("program-%d/fake-file-key", programId));
    file.save();
    return file;
  }
}
