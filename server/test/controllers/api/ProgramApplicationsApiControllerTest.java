package controllers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import auth.ApiKeyGrants;
import auth.UnauthorizedApiRequestException;
import com.jayway.jsonpath.DocumentContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import models.ApiKeyModel;
import models.ApplicationModel;
import models.ProgramModel;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import services.applicant.JsonPathProvider;
import services.export.AbstractExporterTest;

public class ProgramApplicationsApiControllerTest extends AbstractExporterTest {

  private static final String keyId = "key-id";
  private static final String keySecret = "key-secret";
  private static final String rawCredentials = keyId + ":" + keySecret;
  private static final String serializedApiKey =
      Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
  private ApiKeyModel apiKey;
  private ApplicationModel januaryApplication;
  private ApplicationModel februaryApplication;
  private ApplicationModel marchApplication;

  @Before
  public void setUp() {
    // This inherited method creates a program and three applications to it.
    // The applications have submission/creation times of 2022-01-15, 2022-02-15, and 2022-03-15.
    createFakeProgramWithEnumeratorAndAnswerQuestions();
    januaryApplication = applicationOne;
    februaryApplication = applicationTwo;
    marchApplication = applicationThree;

    apiKey = resourceCreator.createActiveApiKey("test-key", keyId, keySecret);
    apiKey
        .getGrants()
        .grantProgramPermission(fakeProgramWithEnumerator.getSlug(), ApiKeyGrants.Permission.READ);
    apiKey.save();
  }

  @Test
  public void list_success_dateRange() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.of("2022-01-15"),
                /* toDate= */ Optional.of("2022-02-15"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    DocumentContext resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    assertThat(resultJson.read("payload.length()", Integer.class)).isEqualTo(1);
    assertThat(resultJson.read("payload[0].application_id", Long.class))
        .isEqualTo(februaryApplication.id);
  }

  @Test
  public void list_success_multipleResultsInAPage() {
    String firstRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(2))
            .url();

    Result result = doRequest(firstRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    DocumentContext resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    assertThat(resultJson.read("payload.length()", Integer.class)).isEqualTo(2);
    assertThat(resultJson.read("payload[0].application_id", Long.class))
        .isEqualTo(marchApplication.id);
    assertThat(resultJson.read("payload[1].application_id", Long.class))
        .isEqualTo(februaryApplication.id);
  }

  @Test
  public void list_success_multiplePages() {
    String firstRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(2))
            .url();

    Result result = doRequest(firstRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    DocumentContext resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    String nextPageToken = resultJson.read("nextPageToken", String.class);
    assertThat(nextPageToken).isNotBlank();

    String secondRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* nextPageToken= */ Optional.of(nextPageToken),
                /* pageSize= */ Optional.empty())
            .url();

    result = doRequest(secondRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    nextPageToken = resultJson.read("$.nextPageToken", String.class);
    assertThat(nextPageToken).isNull();
    assertThat(resultJson.read("payload[0].application_id", Long.class))
        .isEqualTo(januaryApplication.id);
    assertThat(resultJson.read("payload.length()", Integer.class)).isEqualTo(1);
  }

  @Test
  public void list_unauthorized() {
    ProgramModel program = resourceCreator.insertActiveProgram("test-program");

    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    assertThatThrownBy(() -> doRequest(requestUrl))
        .isInstanceOf(UnauthorizedApiRequestException.class)
        .hasMessage("API key key-id does not have access to test-program");
  }

  private Result doRequest(String requestUrl) {
    return route(
        app,
        fakeRequest("GET", requestUrl)
            .remoteAddress("1.1.1.1")
            .header("Authorization", "Basic " + serializedApiKey)
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort()));
  }
}
