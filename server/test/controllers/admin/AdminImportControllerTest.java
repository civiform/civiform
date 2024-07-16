package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static services.migration.ProgramMigrationServiceTest.EXAMPLE_PROGRAM_JSON;
import static support.FakeRequestBuilder.fakeRequestNew;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Database;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.migration.ProgramMigrationService;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;

public class AdminImportControllerTest extends ResetPostgres {
  private AdminImportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
  private Database database;

  @Before
  public void setUp() {
    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
            instanceOf(AdminImportViewPartial.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class));
    database = DB.getDefault();
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.index(fakeRequestNew());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void index_migrationEnabled_ok() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(fakeRequestNew());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import a program");
  }

  @Test
  public void hxImportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.hxImportProgram(fakeRequestNew());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void hxImportProgram_noRequestBody_redirectsToIndex() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.hxImportProgram(fakeRequestNew());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminImportController.index().url());
  }

  @Test
  public void hxImportProgram_malformattedJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programJson", "{\"adminName : \"admin-name\"}")))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_noTopLevelProgramFieldInJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(
                            ImmutableMap.of(
                                "programJson",
                                "{ \"id\" : 32, \"adminName\" : \"admin-name\","
                                    + " \"adminDescription\" : \"description\"}")))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result))
        .containsPattern("JSON did not have a top-level .*program.* field");
  }

  @Test
  public void hxImportProgram_notEnoughInfoToCreateProgramDef_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(
                            ImmutableMap.of(
                                "programJson",
                                "{ \"program\": { \"adminName\" : \"admin-name\","
                                    + " \"adminDescription\" : \"description\"}}")))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_jsonHasAllProgramInfo_resultHasProgramInfo() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programJson", EXAMPLE_PROGRAM_JSON)))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import Sample Program");
    assertThat(contentAsString(result)).contains("import-program-sample");
    assertThat(contentAsString(result)).contains("Screen 1");
  }

  @Test
  public void saveProgram_savesTheProgram() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.saveProgram(
            addCSRFToken(
                    fakeRequest()
                        .method("POST")
                        .bodyForm(ImmutableMap.of("programJson", EXAMPLE_PROGRAM_JSON)))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramModel program =
        database.find(ProgramModel.class).where().eq("name", "import-program-sample").findOne();

    ProgramDefinition programDefinition = program.getProgramDefinition();
    assertThat(programDefinition.externalLink()).isEqualTo("https://github.com/civiform/civiform");
  }
}
