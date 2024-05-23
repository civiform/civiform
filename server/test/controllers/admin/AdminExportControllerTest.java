package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.migration.ProgramMigrationService;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminExportViewPartial;

public class AdminExportControllerTest extends ResetPostgres {
  private AdminExportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

  @Before
  public void setUp() {
    controller =
        new AdminExportController(
            instanceOf(AdminExportView.class),
            instanceOf(AdminExportViewPartial.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            instanceOf(ProgramService.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("export is not enabled");
  }

  @Test
  public void index_migrationEnabled_ok_listsActiveAndDraftPrograms() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Export a program");
    assertThat(contentAsString(result)).contains("active-program-1");
    assertThat(contentAsString(result)).contains("active-program-2");
    assertThat(contentAsString(result)).contains("draft-program");
  }

  @Test
  public void hxExportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.hxExportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("export is not enabled");
  }

  @Test
  public void hxExportProgram_noProgramSelected_redirectsToIndex() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.hxExportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminExportController.index().url());
  }

  @Test
  public void hxExportProgram_invalidProgramId_badRequest() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxExportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programId", String.valueOf(Long.MAX_VALUE))))
                .build());

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("ID " + Long.MAX_VALUE + " could not be found");
  }

  @Test
  public void hxExportProgram_validProgram_rendersHtmxPartial() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("active-program-1").build();

    Result result =
        controller.hxExportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programId", String.valueOf(activeProgram.id))))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Copy Json");
    assertThat(contentAsString(result)).contains("Download Json");
  }

  @Test
  public void downloadJson_downloadsJson() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    String adminName = "fake-admin-name";

    Result result =
        controller.downloadJson(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programJson", String.valueOf(""))))
                .build(),
            adminName);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType().get()).isEqualTo("application/json");
    assertThat(result.header("Content-Disposition")).isPresent();
    assertThat(result.header("Content-Disposition").get())
        .startsWith(String.format("attachment; filename=\"%s-exported", adminName));
  }
}
