package controllers.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.DeploymentType;
import services.docs.ApiDocsService;
import services.openapi.OpenApiVersion;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.admin.docs.SchemaPageView;
import views.shared.LayoutDeps;

public class OpenApiSchemaControllerTest extends ResetPostgres {

  private static final String SCHEMA_UI_NO_PROGRAM_FOUND_ERROR = "No programs found.";

  private OpenApiSchemaController controller;

  @Before
  public void setUp() {
    resetTables();

    controller =
        new OpenApiSchemaController(
            instanceOf(ApiDocsService.class),
            instanceOf(SettingsManifest.class),
            instanceOf(DeploymentType.class),
            new SchemaPageView(layoutDepsWithMockProfile()),
            instanceOf(MessagesApi.class));

    // Create one program in the system
    ProgramBuilder.newActiveProgram("Test Program 1").buildDefinition();
    // Create one external program in the system
    ProgramBuilder.newActiveProgram("Test External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();
  }

  @Test
  public void getSchemaByProgramSlug_loadsActiveProgram() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "test-program-1",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void getSchemaByProgramSlug_scoringTurnedOffInLaterVersion_includesScores() {
    // An older version of the program used scoring, but the current one does not.
    ProgramBuilder.newObsoleteProgram("Scored Then Off").withUsesScoring(true).buildDefinition();
    ProgramBuilder.newActiveProgram("Scored Then Off").buildDefinition();

    Request request =
        fakeRequestBuilder().addCiviFormSetting("ANSWER_OPTION_SCORING_ENABLED", "true").build();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "scored-then-off",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.OPENAPI_V3_0.toString()));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("total_score");
  }

  @Test
  public void getSchemaByProgramSlug_scoringFlagOff_omitsScores() {
    ProgramBuilder.newObsoleteProgram("Scored Then Off").withUsesScoring(true).buildDefinition();
    ProgramBuilder.newActiveProgram("Scored Then Off").buildDefinition();

    Result result =
        controller.getSchemaByProgramSlug(
            fakeRequest(),
            "scored-then-off",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.OPENAPI_V3_0.toString()));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("total_score");
  }

  @Test
  public void getSchemaByProgramSlug_loadsDraftProgram() {
    ProgramBuilder.newDraftProgram("Test Program 1");

    Request request = fakeRequest();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "test-program-1",
            Optional.of(LifecycleStage.DRAFT.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void getSchemaByProgramSlug_cannotFindProgram() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "test-program-2",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void getSchemaByProgramSlug_cannotFindOpenApiVersion() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "test-program-1",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of("unknown-version"));

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void getSchemaByProgramSlug_externalProgram_cannotFindProgram() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaByProgramSlug(
            request,
            "test-external-program",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void getSchemaUI_loadsActiveProgram() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaUI(
            request,
            "test-program-1",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of("unknown-version"));

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void getSchemaUI_noProgram_loadsArbitraryActive() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaUI(
            request,
            "test-program-2",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(SCHEMA_UI_NO_PROGRAM_FOUND_ERROR);
  }

  @Test
  public void getSchemaUI_externalProgram_loadsArbitraryActive() {
    Request request = fakeRequest();
    Result result =
        controller.getSchemaUI(
            request,
            "test-external-program",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(SCHEMA_UI_NO_PROGRAM_FOUND_ERROR);
    assertThat(contentAsString(result)).doesNotContain("test-external-program");
  }

  @Test
  public void getSchemaUI_externalProgramsOnly_noProgramFound() {
    resetTables();
    ProgramBuilder.newActiveProgram("Test External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Request request = fakeRequest();
    Result result =
        controller.getSchemaUI(
            request,
            "",
            Optional.of(LifecycleStage.ACTIVE.getValue()),
            Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(SCHEMA_UI_NO_PROGRAM_FOUND_ERROR);
  }

  /**
   * Builds a {@link LayoutDeps} whose {@link ProfileUtils} returns a mocked profile. The Thymeleaf
   * page views render the admin header from the current user's profile, which a fake request does
   * not carry.
   */
  private LayoutDeps layoutDepsWithMockProfile() {
    ProfileUtils profileUtils = mock(ProfileUtils.class);
    when(profileUtils.currentUserProfile(any())).thenReturn(mock(CiviFormProfile.class));

    LayoutDeps injectedLayoutDeps = instanceOf(LayoutDeps.class);
    return new LayoutDeps(
        injectedLayoutDeps.baseViewDeps(),
        injectedLayoutDeps.bundledAssetsFinder(),
        profileUtils,
        injectedLayoutDeps.deploymentType(),
        injectedLayoutDeps.clock());
  }
}
