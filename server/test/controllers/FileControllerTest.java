package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static support.FakeRequestBuilder.fakeRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import models.ApplicantModel;
import models.ProgramModel;
import models.StoredFileModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.settings.SettingsManifest;
import support.ProgramBuilder;

public class FileControllerTest extends WithMockedProfiles {

  private FileController controller;
  private SettingsManifest mockSettingsManifest;
  private final Request request = fakeRequest();

  @Before
  public void setUp() {
    resetDatabase();
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getClientIpType()).thenReturn(Optional.of("FORWARDED"));
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(false);
    setupInjectorWithExtraBinding(bind(SettingsManifest.class).toInstance(mockSettingsManifest));
    controller = instanceOf(FileController.class);
  }

  @Test
  public void show_differentApplicant_returnsUnauthorizedResult() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id, 1L);
    Result result =
        controller.show(request, applicant.id + 1, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void show_differentFileKey_returnsNotFound() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id + 1, 1L);
    Result result = controller.show(request, applicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void show_TIManagedApplicant_redirects() {
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    String fileKey = fakeFileKey(managedApplicant.id, 1L);
    Result result =
        controller.show(request, managedApplicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void show_applicant_redirects() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    String fileKey = fakeFileKey(applicant.id, 1L);
    Result result = controller.show(request, applicant.id, fileKey).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_programAdmin_invalidProgram_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id);
    Result result = controller.adminShow(request, program.id + 1, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_globalAdmin_invalidProgram_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    Result result = controller.adminShow(request, program.id + 1, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_programAdmin_differentProgram_returnsUnauthorizedResult() {
    ProgramModel programOne = ProgramBuilder.newDraftProgram("one").build();
    ProgramModel programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    String fileKey = fakeFileKey(1L, programTwo.id);
    createStoredFileWithProgramAccess(fileKey, programTwo);

    Result result = controller.adminShow(request, programTwo.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_programAdmin_differentFileKey_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id + 1);
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_globalAdminWithPrivledges_differentFileKey_returnsNotFound() {
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(true);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id + 1);
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void adminShow_globalAdmin_returnsUnauthorizedResult() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_globalAdminWhenNoProgramAdmin_returnsUnauthorizedResult() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void adminShow_programAdmin_redirects() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void adminShow_globalAdminWithPrivledges_redirects() {
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(true);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.adminShow(request, program.id, fileKey);
    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void acledAdminShow_programAdmin_differentProgram_returnsUnauthorizedResult() {
    ProgramModel programOne = ProgramBuilder.newDraftProgram("one").build();
    ProgramModel programTwo = ProgramBuilder.newDraftProgram("two").build();
    createProgramAdminWithMockedProfile(programOne);
    String programTwoFileKey = fakeFileKey(1L, programTwo.id);
    createStoredFileWithProgramAccess(fakeFileKey(1L, programOne.id), programOne);
    createStoredFileWithProgramAccess(programTwoFileKey, programTwo);

    Result result = controller.acledAdminShow(request, programTwoFileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void acledAdminShow_programAdmin_differentFileKey_returnsNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    String fileKey = fakeFileKey(1L, program.id + 1);
    Result result = controller.acledAdminShow(request, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void acledAdminShow_globalAdminWithPrivledges_differentFileKey_returnsNotFound() {
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(true);
    createGlobalAdminWithMockedProfile();
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createStoredFileWithProgramAccess(fakeFileKey(1L, program.id), program);

    String fileKey = fakeFileKey(1L, program.id + 1);
    Result result = controller.acledAdminShow(request, fileKey);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void acledAdminShow_globalAdminWithoutPrivledges_returnsUnauthorizedResult() {
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(false);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);

    Result result = controller.acledAdminShow(request, fileKey);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void acledAdminShow_globalAdminWithPrivledges_redirects() {
    when(mockSettingsManifest.getAllowCiviformAdminAccessPrograms(request)).thenReturn(true);
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createGlobalAdminWithMockedProfile();
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);
    String encodedFileKey = encodefakeFileKey(fileKey);

    Result result = controller.acledAdminShow(request, encodedFileKey);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  @Test
  public void acledAdminShow_programAdmin_redirects() {
    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    createProgramAdminWithMockedProfile(program);
    String fileKey = fakeFileKey(1L, program.id);
    createStoredFileWithProgramAccess(fileKey, program);
    String encodedFileKey = encodefakeFileKey(fileKey);

    Result result = controller.acledAdminShow(request, encodedFileKey);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
  }

  private String fakeFileKey(long applicantId, long programId) {
    return String.format("applicant-%d/program-%d/block-0", applicantId, programId);
  }

  private String encodefakeFileKey(String fileKey) {
    return URLEncoder.encode(fileKey, StandardCharsets.UTF_8);
  }

  private void createStoredFileWithProgramAccess(String fileKey, ProgramModel program) {
    var file = new StoredFileModel().setName(fileKey);
    file.getAcls().addProgramToReaders(program.getProgramDefinition());
    file.save();
  }
}
