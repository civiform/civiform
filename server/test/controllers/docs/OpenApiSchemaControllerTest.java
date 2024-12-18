package controllers.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static support.FakeRequestBuilder.fakeRequest;

import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.openapi.OpenApiVersion;
import support.ProgramBuilder;

public class OpenApiSchemaControllerTest extends ResetPostgres {
  @Before
  public void setUp() {
    resetTables();

    // Create one program in the system
    ProgramBuilder.newActiveProgram("Test Program 1").buildDefinition();
  }

  @Test
  public void getSchemaByProgramSlug_loadsActiveProgram() {
    Request request = fakeRequest();
    Result result =
        instanceOf(OpenApiSchemaController.class)
            .getSchemaByProgramSlug(
                request,
                "test-program-1",
                Optional.of(LifecycleStage.ACTIVE.getValue()),
                Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void getSchemaByProgramSlug_loadsDraftProgram() {
    ProgramBuilder.newDraftProgram("Test Program 1");

    Request request = fakeRequest();
    Result result =
        instanceOf(OpenApiSchemaController.class)
            .getSchemaByProgramSlug(
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
        instanceOf(OpenApiSchemaController.class)
            .getSchemaByProgramSlug(
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
        instanceOf(OpenApiSchemaController.class)
            .getSchemaByProgramSlug(
                request,
                "test-program-1",
                Optional.of(LifecycleStage.ACTIVE.getValue()),
                Optional.of("unknown-version"));

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void getSchemaUI_loadsActiveProgram() {
    Request request = fakeRequest();
    Result result =
        instanceOf(OpenApiSchemaController.class)
            .getSchemaUI(
                request,
                "test-program-1",
                Optional.of(LifecycleStage.ACTIVE.getValue()),
                Optional.of("unknown-version"));

    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void getSchemaUI_cannotFindProgram() {
    Request request = fakeRequest();
    Result result =
        instanceOf(OpenApiSchemaController.class)
            .getSchemaByProgramSlug(
                request,
                "test-program-2",
                Optional.of(LifecycleStage.ACTIVE.getValue()),
                Optional.of(OpenApiVersion.SWAGGER_V2.toString()));

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
