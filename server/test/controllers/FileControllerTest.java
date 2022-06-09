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
  private Request request = fakeRequest().build();

  @Before
  public void setUp() {
    resetDatabase();
    controller = instanceOf(FileController.class);
  }

  @Test
  public void show_differentApplicant_returnsUnauthorizedResult() {
    Applicant applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id, 1L);
    Result result =
        controller.show(request, applicant.id + 1, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void show_differentFileKey_returnsNotFound() {
    Applicant applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id + 1, 1L);
    Result result = controller.show(request, applicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void show_TIManagedApplicant_redirects() {
    Applicant managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    String fileKey = fakeFileKey(managedApplicant.id, 1L);
    Result result =
        controller.show(request, managedApplicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void show_redirects() {
    Applicant applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id, 1L);
    Result result = controller.show(request, applicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_differentProgram_returnsUnauthorizedResult() {
    Program programOne = ProgramBuilder.newDraftProgram("one").build();
    Program programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    String fileKey = fakeFileKey(1L, programTwo.id);
    createStoredFileWithProgramAccess(fileKey, programTwo);

    Result result = controller.adminShow(request, programTwo.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_differentFileKey_returnsNotFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id + 1);
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_globalAdmin_returnsUnauthorizedResult() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_globalAdminWhenNoProgramAdmin_returnsUnauthorizedResult() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void acledAdminShow_differentProgram_returnsUnauthorizedResult() {
    Program programOne = ProgramBuilder.newDraftProgram("one").build();
    Program programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    String programTwoFileKey = fakeFileKey(1L, programTwo.id);
    createStoredFileWithProgramAccess(fakeFileKey(1L, programOne.id), programOne);
    createStoredFileWithProgramAccess(programTwoFileKey, programTwo);

    Result result = controller.acledAdminShow(request, programTwoFileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void acledAdminShow_differentFileKey_returnsNotFound() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    String fileKey = fakeFileKey(1L, program.id + 1);
    Result result = controller.acledAdminShow(request, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void acledAdminShow_globalAdmin_returnsUnauthorizedResult() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.acledAdminShow(request, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void acledAdminShow_redirects() {
    Program program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.acledAdminShow(fakeRequest().build(), fileKey);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  private String fakeFileKey(long applicantId, long programId) {
    return String.format("applicant-%d/program-%d/block-0", applicantId, programId);
  }

  private void createStoredFileWithProgramAccess(String fileKey, Program program) {
    var file = new StoredFile().setName(fileKey);
    file.getAcls().addProgramToReaders(program.getProgramDefinition());
    file.save();
  }
}
