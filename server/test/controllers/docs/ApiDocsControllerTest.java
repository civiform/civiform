package controllers.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
import services.program.ProgramType;
import support.ProgramBuilder;

public class ApiDocsControllerTest extends ResetPostgres {

  private static final String PROGRAM_OR_VERSION_NOT_FOUND_ERROR = "Program and version not found";

  @Before
  public void setUp() {
    resetTables();

    // Create one program in the system
    ProgramBuilder.newActiveProgram("Test Program 1").buildDefinition();
  }

  @Test
  public void index_redirectsToArbitraryProgramActiveDocs() {
    Request request = fakeRequest();
    Result result = instanceOf(ApiDocsController.class).index(request);

    // SEE_OTHER is the redirect code
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .isEqualTo(Optional.of("/docs/api/programs/test-program-1/active"));
  }

  @Test
  public void index_externalProgramOnly_notFound() {
    resetTables();
    ProgramBuilder.newActiveProgram("Test External Program 1")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();

    Request request = fakeRequest();
    Result result = instanceOf(ApiDocsController.class).index(request);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("No programs found");
  }

  @Test
  public void activeDocsForSlug_programExists() {
    Request request = fakeRequest();
    Result result =
        instanceOf(ApiDocsController.class).activeDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void activeDocsForSlug_programDoesNotExist() {
    Request request = fakeRequest();
    Result result =
        instanceOf(ApiDocsController.class).activeDocsForSlug(request, "non-existent-program");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void activeDocsForSlug_externalProgram_doesNotExist() {
    ProgramBuilder.newActiveProgram("Test External Program")
        .withProgramType(ProgramType.EXTERNAL)
        .buildDefinition();
    Request request = fakeRequest();
    Result result =
        instanceOf(ApiDocsController.class).activeDocsForSlug(request, "test-external-program");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_noDraftAvailable() {
    Request request = fakeRequest();
    Result result = instanceOf(ApiDocsController.class).draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_draftAvailable() {
    ProgramBuilder.newDraftProgram("Test Program 1").buildDefinition();

    Request request = fakeRequest();
    Result result = instanceOf(ApiDocsController.class).draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }
}
