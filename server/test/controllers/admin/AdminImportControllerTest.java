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

import auth.ProfileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;

public class AdminImportControllerTest extends ResetPostgres {
  private AdminImportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

  @Before
  public void setUp() {
    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
            instanceOf(AdminImportViewPartial.class),
            instanceOf(FormFactory.class),
            instanceOf(ObjectMapper.class),
            instanceOf(ProfileUtils.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class));
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void index_migrationEnabled_ok() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import a program");
  }

  @Test
  public void hxImportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.hxImportProgram(addCSRFToken(fakeRequest()).build());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void hxImportProgram_noRequestBody_redirectsToIndex() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.hxImportProgram(addCSRFToken(fakeRequest()).build());

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
                        .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON)))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import Sample Program");
    assertThat(contentAsString(result)).contains("import-program-sample");
    assertThat(contentAsString(result)).contains("Screen 1");
  }

  /**
   * This contains the bare minimum needed to parse JSON into a program definition. The
   * admin_program_migration.test.ts browser test has tests for a program with many blocks and
   * questions.
   */
  private static final String PROGRAM_JSON =
      "{\n"
          + "  \"program\": {\n"
          + "    \"id\": 34,\n"
          + "    \"adminName\": \"import-program-sample\",\n"
          + "    \"adminDescription\": \"desc\",\n"
          + "    \"externalLink\": \"https://github.com/civiform/civiform\",\n"
          + "    \"displayMode\": \"PUBLIC\",\n"
          + "    \"localizedName\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"Import Sample Program\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"localizedDescription\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"A sample program for testing program import\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"localizedConfirmationMessage\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"programType\": \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\": true,\n"
          + "    \"acls\": {\n"
          + "      \"tiProgramViewAcls\": []\n"
          + "    },\n"
          + "    \"localizedSummaryImageDescription\": {\n"
          + "      \"translations\": {\n"
          + "        \"en_US\": \"Test summary image description\"\n"
          + "      },\n"
          + "      \"isRequired\": true\n"
          + "    },\n"
          + "    \"blockDefinitions\": [\n"
          + "      {\n"
          + "        \"id\": 1,\n"
          + "        \"name\": \"Screen 1\",\n"
          + "        \"description\": \"block 1\",\n"
          + "        \"repeaterId\": null,\n"
          + "        \"hidePredicate\": null,\n"
          + "        \"optionalPredicate\": null,\n"
          + "        \"questionDefinitions\": [\n"
          + "          {\n"
          + "            \"id\": 18,\n"
          + "            \"optional\": false,\n"
          + "            \"addressCorrectionEnabled\": false\n"
          + "          },\n"
          + "          {\n"
          + "            \"id\": 5,\n"
          + "            \"optional\": false,\n"
          + "            \"addressCorrectionEnabled\": true\n"
          + "          }\n"
          + "        ]\n"
          + "      }\n"
          + "      ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    }"
          + "  }\n"
          + "}\n";
}
