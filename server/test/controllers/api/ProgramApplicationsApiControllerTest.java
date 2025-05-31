package controllers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ApiKeyGrants;
import auth.UnauthorizedApiRequestException;
import com.jayway.jsonpath.DocumentContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import models.ApiKeyModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
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

  /**
   * Tests that the Program Applications API correctly filters applications by date range,
   * supporting both date-only and full timestamp filtering.
   */
  @Test
  public void list_success_timedDateRange() {
    ApplicantModel newApplicant = resourceCreator.insertApplicantWithAccount();

    // Create morning application (submitted at 8:30 AM UTC on 2025-01-01)
    ApplicationModel morningApplication =
        new ApplicationModel(newApplicant, fakeProgramWithEnumerator, LifecycleStage.OBSOLETE);
    morningApplication.setApplicantData(newApplicant.getApplicantData());
    morningApplication.setCreateTimeForTest(Instant.parse("2025-01-01T08:00:00.00Z"));
    morningApplication.setSubmitTimeForTest(Instant.parse("2025-01-01T08:30:00.00Z"));
    morningApplication.save();

    // Create evening application (submitted at 4:30 PM UTC on 2025-01-01)
    ApplicationModel eveningApplication =
        new ApplicationModel(newApplicant, fakeProgramWithEnumerator, LifecycleStage.ACTIVE);
    eveningApplication.setApplicantData(newApplicant.getApplicantData());
    eveningApplication.setCreateTimeForTest(Instant.parse("2025-01-01T16:00:00.00Z"));
    eveningApplication.setSubmitTimeForTest(Instant.parse("2025-01-01T16:30:30.00Z"));
    eveningApplication.save();

    // Test Case 1: Date-only filtering (should include entire day range)
    // Using date-only format should capture all applications from 2025-01-01
    String dateOnlyRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.of("2025-01-01"),
                /* toDate= */ Optional.of("2025-01-02"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(dateOnlyRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    // Verify both applications are returned when using date-only filtering
    DocumentContext resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    assertThat(resultJson.read("payload.length()", Integer.class)).isEqualTo(2);

    // Results should be ordered by submission time (most recent first)
    assertThat(resultJson.read("payload[0].application_id", Long.class))
        .isEqualTo(eveningApplication.id);
    assertThat(resultJson.read("payload[1].application_id", Long.class))
        .isEqualTo(morningApplication.id);

    // Test Case 2: Precise timestamp filtering (should only include applications within time
    // window)
    // Time range from 6:00 AM to 10:00 AM UTC should only capture the morning application
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                fakeProgramWithEnumerator.getSlug(),
                /* fromDate= */ Optional.of("2025-01-01T06:00:00.00Z"),
                /* toDate= */ Optional.of("2025-01-01T10:00:00.00Z"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    // Verify only the morning application is returned (evening app at 4:30 PM is outside range)
    resultJson = JsonPathProvider.getJsonPath().parse(contentAsString(result));
    assertThat(resultJson.read("payload.length()", Integer.class)).isEqualTo(1);
    assertThat(resultJson.read("payload[0].application_id", Long.class))
        .isEqualTo(morningApplication.id);
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
        fakeRequestBuilder()
            .method("GET")
            .uri(requestUrl)
            .remoteAddress("1.1.1.1")
            .header("Authorization", "Basic " + serializedApiKey)
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort()));
  }
}
