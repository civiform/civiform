package controllers.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.Helpers.testServerPort;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ApiKeyGrants;
import auth.UnauthorizedApiRequestException;
import com.jayway.jsonpath.DocumentContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
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
import services.program.ProgramType;
import support.ProgramBuilder;

public class ProgramApplicationsApiControllerTest extends AbstractExporterTest {

  private static final String keyId = "key-id";
  private static final String keySecret = "key-secret";
  private static final String rawCredentials = keyId + ":" + keySecret;
  private static final String serializedApiKey =
      Base64.getEncoder().encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));
  private ApiKeyModel apiKey;
  private ProgramModel program;
  private ProgramModel externalProgram;
  private ApplicationModel januaryApplication;
  private ApplicationModel februaryApplication;
  private ApplicationModel marchApplication;

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

  private DocumentContext parseResult(Result result) {
    return JsonPathProvider.getJsonPath().parse(contentAsString(result));
  }

  private Integer getPayloadLength(DocumentContext resultJson) {
    return resultJson.read("payload.length()", Integer.class);
  }

  private List<Integer> getApplicationIds(DocumentContext resultJson) {
    return resultJson.read("payload[*].application_id");
  }

  @Before
  public void setUp() {
    program = ProgramBuilder.newActiveProgram().withName("Fake Program").build();
    externalProgram =
        ProgramBuilder.newActiveProgram()
            .withName("Fake External Program")
            .withProgramType(ProgramType.EXTERNAL)
            .build();

    ApplicantModel applicantOne = resourceCreator.insertApplicantWithAccount();
    ApplicantModel applicantTwo = resourceCreator.insertApplicantWithAccount();

    // Create three applications with submission times one month apart at 12:30 UTC.
    // The 12:30 UTC time is intentionally chosen so that date-only queries (which default
    // to beginning of day in local time) will match on the same day for these applications. For
    // example, a query for "2022-01-01" without timezone defaults to "2022-01-01T00:00:00" in
    // local time. In PST (UTC-8), this becomes "2022-01-01T04:30:00Z", which is still on the same
    // day.
    januaryApplication = new ApplicationModel(applicantOne, program, LifecycleStage.ACTIVE);
    januaryApplication.setApplicantData(applicantOne.getApplicantData());
    januaryApplication.setCreateTimeForTest(Instant.parse("2022-01-01T12:00:00.00Z"));
    januaryApplication.setSubmitTimeForTest(Instant.parse("2022-01-01T12:30:00.00Z"));
    januaryApplication.save();

    februaryApplication = new ApplicationModel(applicantTwo, program, LifecycleStage.OBSOLETE);
    februaryApplication.setApplicantData(applicantTwo.getApplicantData());
    februaryApplication.setCreateTimeForTest(Instant.parse("2022-02-01T12:00:00.00Z"));
    februaryApplication.setSubmitTimeForTest(Instant.parse("2022-02-01T12:30:00.00Z"));
    februaryApplication.save();

    marchApplication = new ApplicationModel(applicantTwo, program, LifecycleStage.ACTIVE);
    marchApplication.setApplicantData(applicantTwo.getApplicantData());
    marchApplication.setCreateTimeForTest(Instant.parse("2022-03-01T12:00:00.00Z"));
    marchApplication.setSubmitTimeForTest(Instant.parse("2022-03-01T12:00:30.00Z"));
    marchApplication.save();

    apiKey = resourceCreator.createActiveApiKey("test-key", keyId, keySecret);
    apiKey.getGrants().grantProgramPermission(program.getSlug(), ApiKeyGrants.Permission.READ);
    apiKey
        .getGrants()
        .grantProgramPermission(externalProgram.getSlug(), ApiKeyGrants.Permission.READ);
    apiKey.save();
  }

  /** Test listing all applications without any filters. */
  @Test
  public void list_success_allApplications() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Should return all applications from most recent to oldest
    assertThat(getPayloadLength(resultJson)).isEqualTo(3);
    assertThat(getApplicationIds(resultJson))
        .containsExactly(
            marchApplication.id.intValue(),
            februaryApplication.id.intValue(),
            januaryApplication.id.intValue());
  }

  /** Test filtering applications from a specific date onwards. */
  @Test
  public void list_success_fromDateOnly() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-02-01"),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Should return February and March applications (inclusive start date)
    assertThat(getPayloadLength(resultJson)).isEqualTo(2);
    assertThat(getApplicationIds(resultJson))
        .containsExactly(marchApplication.id.intValue(), februaryApplication.id.intValue());
  }

  /** Test filtering applications up to a specific date. */
  @Test
  public void list_success_toDateOnly() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.of("2022-02-01"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Should return only January application (exclusive end date)
    assertThat(getPayloadLength(resultJson)).isEqualTo(1);
    assertThat(getApplicationIds(resultJson)).containsExactly(januaryApplication.id.intValue());
  }

  /** Test filtering applications by date range. */
  @Test
  public void list_success_dateRange() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-15"),
                /* toDate= */ Optional.of("2022-02-15"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Should return only February application
    assertThat(getPayloadLength(resultJson)).isEqualTo(1);
    assertThat(getApplicationIds(resultJson)).containsExactly(februaryApplication.id.intValue());
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
        new ApplicationModel(newApplicant, program, LifecycleStage.OBSOLETE);
    morningApplication.setApplicantData(newApplicant.getApplicantData());
    morningApplication.setCreateTimeForTest(Instant.parse("2025-01-01T08:00:00.00Z"));
    morningApplication.setSubmitTimeForTest(Instant.parse("2025-01-01T08:30:00.00Z"));
    morningApplication.save();

    // Create evening application (submitted at 4:30 PM UTC on 2025-01-01)
    ApplicationModel eveningApplication =
        new ApplicationModel(newApplicant, program, LifecycleStage.ACTIVE);
    eveningApplication.setApplicantData(newApplicant.getApplicantData());
    eveningApplication.setCreateTimeForTest(Instant.parse("2025-01-01T16:00:00.00Z"));
    eveningApplication.setSubmitTimeForTest(Instant.parse("2025-01-01T16:30:30.00Z"));
    eveningApplication.save();

    // Test Case 1: Date-only filtering (should include entire day range)
    // Using date-only format should capture all applications from 2025-01-01
    String dateOnlyRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2025-01-01"),
                /* toDate= */ Optional.of("2025-01-02"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(dateOnlyRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    // Verify both applications are returned when using date-only filtering
    DocumentContext resultJson = parseResult(result);
    assertThat(getPayloadLength(resultJson)).isEqualTo(2);

    // Results should be ordered by submission time (most recent first)
    assertThat(getApplicationIds(resultJson))
        .containsExactly(eveningApplication.id.intValue(), morningApplication.id.intValue());

    // Test Case 2: Precise timestamp filtering (should only include applications within time
    // window)
    // Time range from 6:00 AM to 10:00 AM UTC should only capture the morning application
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2025-01-01T06:00:00.00Z"),
                /* toDate= */ Optional.of("2025-01-01T10:00:00.00Z"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);

    // Verify only the morning application is returned (evening app at 4:30 PM is outside range)
    resultJson = parseResult(result);
    assertThat(getPayloadLength(resultJson)).isEqualTo(1);
    assertThat(getApplicationIds(resultJson)).containsExactly(morningApplication.id.intValue());
  }

  /** Test filtering by revision state "CURRENT" which maps to ACTIVE lifecycle stage. */
  @Test
  public void list_success_withRevisionStateCurrent() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.of("CURRENT"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    assertThat(getPayloadLength(resultJson)).isEqualTo(2);
    assertThat(getApplicationIds(resultJson))
        .containsExactlyInAnyOrder(
            januaryApplication.id.intValue(), marchApplication.id.intValue());
  }

  /** Test filtering by revision state "OBSOLETE" which maps to OBSOLETE lifecycle stage. */
  @Test
  public void list_success_withRevisionStateObsolete() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.of("OBSOLETE"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Should return applications with OBSOLETE lifecycle stage (February only)
    assertThat(getPayloadLength(resultJson)).isEqualTo(1);
    assertThat(getApplicationIds(resultJson))
        .containsExactlyInAnyOrder(februaryApplication.id.intValue());
  }

  /** Test pagination with custom page size and verify next page token generation. */
  @Test
  public void list_success_pageSize() {
    // Add a request that should returns three applications
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(2))
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Since page size is set to two, only two applications should be returned,
    assertThat(getPayloadLength(resultJson)).isEqualTo(2);
    assertThat(getApplicationIds(resultJson))
        .containsExactly(marchApplication.id.intValue(), februaryApplication.id.intValue());

    // and there should be a next page token that includes the remaining application
    String nextPageToken = resultJson.read("nextPageToken", String.class);
    assertThat(nextPageToken).isNotBlank();
  }

  /** Test listing applications across multiple pages. */
  @Test
  public void list_success_multiplePages() {
    // Add a request that should return two applications in separate pages
    String firstRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-12-31"),
                /* revisionState= */ Optional.of("CURRENT"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(1))
            .url();

    Result result = doRequest(firstRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Verify first request has March application and a next page token
    assertThat(getApplicationIds(resultJson)).containsExactly(marchApplication.id.intValue());
    String nextPageToken = resultJson.read("nextPageToken", String.class);
    assertThat(nextPageToken).isNotBlank();

    // Make second request with the next page token and the same date range
    String secondRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-12-31"),
                /* revisionState= */ Optional.of("CURRENT"),
                /* nextPageToken= */ Optional.of(nextPageToken),
                /* pageSize= */ Optional.empty())
            .url();

    result = doRequest(secondRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    resultJson = parseResult(result);

    // Verify second request has January application and no next page token
    assertThat(getApplicationIds(resultJson)).containsExactly(januaryApplication.id.intValue());
    nextPageToken = resultJson.read("$.nextPageToken", String.class);
    assertThat(nextPageToken).isNull();
  }

  @Test
  public void list_error_invalidFromDate() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("invalid-date"),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    assertThatThrownBy(() -> doRequest(requestUrl))
        .isInstanceOf(BadApiRequestException.class)
        .hasMessage("Malformed query param: fromDate");
  }

  /** Test error handling for malformed toDate parameter. */
  @Test
  public void list_error_invalidToDate() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.of("01-01-2022"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    assertThatThrownBy(() -> doRequest(requestUrl))
        .isInstanceOf(BadApiRequestException.class)
        .hasMessage("Malformed query param: toDate");
  }

  /** Test error handling for invalid revision state parameter. */
  @Test
  public void list_error_invalidRevisionState() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.of("INVALID_STATE"),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    assertThatThrownBy(() -> doRequest(requestUrl))
        .isInstanceOf(BadApiRequestException.class)
        .hasMessage("Invalid revision state parameter: INVALID_STATE");
  }

  /**
   * Test error handling for unauthorized access to a program without proper API key permissions.
   */
  @Test
  public void list_error_unauthorizedProgram() {
    ProgramModel newProgram = resourceCreator.insertActiveProgram("test-program");

    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                newProgram.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    assertThatThrownBy(() -> doRequest(requestUrl))
        .isInstanceOf(UnauthorizedApiRequestException.class)
        .hasMessage("API key key-id does not have access to test-program");
  }

  /**
   * Test error handling for pagination token that references a request with a different date param.
   */
  @Test
  public void list_error_mismatchedPaginationTokenDateParam() {
    // Add a request that should return two applications in separate pages
    String firstRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-02-15"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(1))
            .url();

    Result result = doRequest(firstRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Verify first request has February application and a next page token
    assertThat(getApplicationIds(resultJson)).containsExactly(februaryApplication.id.intValue());
    String nextPageToken = resultJson.read("nextPageToken", String.class);
    assertThat(nextPageToken).isNotBlank();

    // Make second request with a different date range
    String secondRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-04-30"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.of(nextPageToken),
                /* pageSize= */ Optional.empty())
            .url();

    // Request should fail because request parameters must match pagination token
    assertThatThrownBy(() -> doRequest(secondRequestUrl))
        .isInstanceOf(BadApiRequestException.class)
        .hasMessage("Request parameters must match pagination token: toDate");
  }

  /**
   * Test error handling for pagination token that references a request with a different pageSize
   * param.
   */
  @Test
  public void list_error_mismatchedPaginationTokenPageSizeParam() {
    // Add a request that should return two applications in separate pages
    String firstRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-02-15"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.of(1))
            .url();

    Result result = doRequest(firstRequestUrl);
    assertThat(result.status()).isEqualTo(HttpStatus.SC_OK);
    DocumentContext resultJson = parseResult(result);

    // Verify first request has February application and a next page token
    assertThat(getApplicationIds(resultJson)).containsExactly(februaryApplication.id.intValue());
    String nextPageToken = resultJson.read("nextPageToken", String.class);
    assertThat(nextPageToken).isNotBlank();

    // Make second request with a different page size
    String secondRequestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                program.getSlug(),
                /* fromDate= */ Optional.of("2022-01-01"),
                /* toDate= */ Optional.of("2022-02-15"),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.of(nextPageToken),
                /* pageSize= */ Optional.of(6))
            .url();

    // Request should fail because request parameters must match pagination token
    assertThatThrownBy(() -> doRequest(secondRequestUrl))
        .isInstanceOf(BadApiRequestException.class)
        .hasMessage("Request parameters must match pagination token: pageSize");
  }

  @Test
  public void list_externalProgram_returnsBadRequest() {
    String requestUrl =
        controllers.api.routes.ProgramApplicationsApiController.list(
                externalProgram.getSlug(),
                /* fromDate= */ Optional.empty(),
                /* toDate= */ Optional.empty(),
                /* revisionState= */ Optional.empty(),
                /* nextPageToken= */ Optional.empty(),
                /* pageSize= */ Optional.empty())
            .url();

    Result result = doRequest(requestUrl);
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }
}
