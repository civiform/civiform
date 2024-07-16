package controllers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestNew;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;
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
    Request request = fakeRequestNew();
    Result result = instanceOf(ApiDocsController.class).index(request);

    // SEE_OTHER is the redirect code
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .isEqualTo(Optional.of("/api/docs/v1/test-program-1/active"));
  }

  @Test
  public void activeDocsForSlug_programExists() {
    Request request = fakeRequestNew();
    Result result =
        instanceOf(ApiDocsController.class).activeDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void activeDocsForSlug_programDoesNotExist() {
    Request request = fakeRequestNew();
    Result result =
        instanceOf(ApiDocsController.class).activeDocsForSlug(request, "non-existent-program");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_noDraftAvailable() {
    Request request = fakeRequestNew();
    Result result = instanceOf(ApiDocsController.class).draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }

  @Test
  public void draftDocsForSlug_draftAvailable() {
    ProgramBuilder.newDraftProgram("Test Program 1").buildDefinition();

    Request request = fakeRequestNew();
    Result result = instanceOf(ApiDocsController.class).draftDocsForSlug(request, "test-program-1");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain(PROGRAM_OR_VERSION_NOT_FOUND_ERROR);
  }
}
