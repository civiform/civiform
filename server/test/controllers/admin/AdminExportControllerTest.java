package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminExportView;

public class AdminExportControllerTest extends ResetPostgres {
  private AdminExportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

  @Before
  public void setUp() {
    controller =
        new AdminExportController(
            instanceOf(AdminExportView.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigration(any())).thenReturn(false);

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("not enabled");
  }

  @Test
  public void index_migrationEnabled_ok_listsActiveProgramsOnly() {
    when(mockSettingsManifest.getProgramMigration(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Export a program");
    assertThat(contentAsString(result)).contains("active-program-1");
    assertThat(contentAsString(result)).contains("active-program-2");
    assertThat(contentAsString(result)).doesNotContain("draft-program");
  }

  @Test
  public void exportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigration(any())).thenReturn(false);

    Result result = controller.exportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("not enabled");
  }

  @Test
  public void exportProgram_migrationEnabled_notFound_hasProgramId() {
    when(mockSettingsManifest.getProgramMigration(any())).thenReturn(true);

    Result result =
        controller.exportProgram(
            addCSRFToken(fakeRequest().method("POST").bodyForm(ImmutableMap.of("programId", "3")))
                .build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("Received ID: 3");
  }
}
