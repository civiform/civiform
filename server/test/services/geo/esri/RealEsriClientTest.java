package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.libs.Json;
import play.libs.ws.WSClient;
import services.geo.esri.EsriTestHelper.TestType;
import services.geo.esri.models.Candidate;
import services.geo.esri.models.FindAddressCandidatesResponse;
import services.settings.SettingsManifest;

@RunWith(JUnitParamsRunner.class)
public class RealEsriClientTest {
  private EsriTestHelper helper;

  @After
  public void tearDown() throws IOException {
    if (helper != null) {
      helper.stopServer();
    }
  }

  @Test
  public void fetchAddressSuggestions() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    FindAddressCandidatesResponse resp = optionalResponse.get();
    assertThat(resp.spatialReference().get().wkid()).isEqualTo(4326);
    assertThat(resp.candidates()).hasSize(5);
  }

  @Test
  public void fetchAddressSuggestionsWorksUsingOldLegacySingleUrlConfigValue() throws Exception {
    // This is the same as the fetchAddressSuggestions test but forces use of the old config
    // setting.
    // Can do away after the removal of the old config setting.
    helper = new EsriTestHelper(TestType.LEGACY_SINGLE_URL_CONFIG_SETTING);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    FindAddressCandidatesResponse resp = optionalResponse.get();
    assertThat(resp.spatialReference().get().wkid()).isEqualTo(4326);
    assertThat(resp.candidates()).hasSize(5);
  }

  @Test
  public void fetchAddressSuggestionsHavingLine2Populated() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD_WITH_LINE_2);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    addressJson.put("line2", "Apt 123");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    FindAddressCandidatesResponse resp = optionalResponse.get();

    Candidate nodeWithLine2 = resp.candidates().get(0);
    assertThat(nodeWithLine2.attributes().subAddr().get()).isEqualTo("Apt 123");
  }

  @Test
  public void fetchAddressSuggestionsWithNoCandidates() throws Exception {
    helper = new EsriTestHelper(TestType.NO_CANDIDATES);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    FindAddressCandidatesResponse resp = optionalResponse.get();
    assertThat(resp.candidates()).isEmpty();
  }

  @Test
  public void fetchAddressSuggestionsWithError() throws Exception {
    helper = new EsriTestHelper(TestType.ERROR);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    assertThat(optionalResponse.isPresent()).isFalse();
  }

  @Test
  public void fetchAddressSuggestionsMultipleUrls() throws Exception {
    // TestType.MULTIPLE_ENDPOINTS configures the test web server with multi endpoints
    // that each return different numbers of results
    helper = new EsriTestHelper(TestType.MULTIPLE_ENDPOINTS);
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", "380 New York St");
    Optional<FindAddressCandidatesResponse> optionalResponse =
        helper.getClient().fetchAddressSuggestions(addressJson).toCompletableFuture().get();
    FindAddressCandidatesResponse resp = optionalResponse.get();
    assertThat(resp.spatialReference().get().wkid()).isEqualTo(4326);

    // For this test this value is the merged combination of candidate results
    // from multiple endpoints.
    int expectedNumberOfCandidates = 8;
    assertThat(resp.candidates()).hasSize(expectedNumberOfCandidates);
  }

  @Test
  public void fetchServiceAreaFeatures() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION);
    Optional<JsonNode> optionalResponse =
        helper
            .getClient()
            .fetchServiceAreaFeatures(EsriTestHelper.LOCATION, "/query")
            .toCompletableFuture()
            .join();
    JsonNode resp = optionalResponse.get();
    ReadContext ctx = JsonPath.parse(resp.toString());
    List<String> features = ctx.read("features[*].attributes.CITYNAME");
    Optional<String> feature = features.stream().filter(val -> "Seattle".equals(val)).findFirst();
    assertThat(feature.isPresent()).isTrue();
    assertThat(feature.get()).isEqualTo("Seattle");
  }

  @Test
  public void fetchServiceAreaFeaturesWithError() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_ERROR);
    Optional<JsonNode> maybeResp =
        helper
            .getClient()
            .fetchServiceAreaFeatures(EsriTestHelper.LOCATION, "/query")
            .toCompletableFuture()
            .join();
    assertThat(maybeResp.isPresent()).isFalse();
  }

  @Test
  @Parameters(method = "getFetchAddressUrlsParams")
  public void verifyFetchAddressUrlsConfigurationLoads(FetchAddressUrlsTestData testData) {
    SettingsManifest mockSettingsManifest = mock();
    EsriServiceAreaValidationConfig mockEsriServiceAreaValidationConfig = mock();
    Clock mockClock = mock();
    WSClient mockWsClient = mock();

    when(mockSettingsManifest.getEsriFindAddressCandidatesUrls())
        .thenReturn(testData.esriFindAddressCandidatesUrls());

    when(mockSettingsManifest.getEsriFindAddressCandidatesUrl())
        .thenReturn(testData.esriFindAddressCandidatesUrl());

    when(mockSettingsManifest.getEsriArcgisApiToken()).thenReturn(testData.esriArcgisApiToken());

    var client =
        new RealEsriClient(
            mockSettingsManifest, mockClock, mockEsriServiceAreaValidationConfig, mockWsClient);

    assertThat(client.ESRI_FIND_ADDRESS_CANDIDATES_URLS.size())
        .isEqualTo(testData.expectedUrlCount());
  }

  private static ImmutableList<Object[]> getFetchAddressUrlsParams() {
    return ImmutableList.of(
        // Expected one item in url list
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.of(
                  ImmutableList.<String>builder()
                      .add("http://subdomain.host1.tld/path/path/path")
                      .build()),
              Optional.empty(),
              Optional.empty(),
              1)
        },
        // Expect two in url list
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.of(
                  ImmutableList.<String>builder()
                      .add("http://subdomain.host1.tld/path/path/path")
                      .add("http://subdomain.host2.tld/path/path/path")
                      .build()),
              Optional.empty(),
              Optional.empty(),
              2)
        },
        // Expect one in url list because arcgis.com is used without a token
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.of(
                  ImmutableList.<String>builder()
                      .add("http://subdomain.host1.tld/path/path/path")
                      .add("http://api.arcgis.com/path/path/path")
                      .build()),
              Optional.empty(),
              Optional.empty(),
              1)
        },
        // Expect two in url list because arcgis.com is used with a token
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.of(
                  ImmutableList.<String>builder()
                      .add("http://subdomain.host1.tld/path/path/path")
                      .add("http://api.arcgis.com/path/path/path")
                      .build()),
              Optional.empty(),
              Optional.of("greetings-i-am-a-token"),
              2)
        },
        // Expect two in url list because new url list is used which overrides anything set in the
        // old url string
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.of(
                  ImmutableList.<String>builder()
                      .add("http://subdomain.host1.tld/path/path/path")
                      .add("http://subdomain.host2.tld/path/path/path")
                      .build()),
              Optional.of("http://subdomain.host3.tld/path/path/path"),
              Optional.empty(),
              2)
        },
        // Expect one in url list because we use the old single value option
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.empty(),
              Optional.of("http://subdomain.host1.tld/path/path/path"),
              Optional.empty(),
              1)
        },
        // Expect zero in url list because we use the old single value option and arcgis.com is used
        // without a token
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.empty(),
              Optional.of("http://api.arcgis.com/path/path/path"),
              Optional.empty(),
              0)
        },
        // Expect one in url list because we use the old single value option and arcgis.com is used
        // with a token
        new Object[] {
          FetchAddressUrlsTestData.create(
              Optional.empty(),
              Optional.of("http://api.arcgis.com/path/path/path"),
              Optional.of("greetings-i-am-a-token"),
              1)
        },
        // Expect zero in url list because we have no urls configured
        new Object[] {
          FetchAddressUrlsTestData.create(Optional.empty(), Optional.empty(), Optional.empty(), 0)
        });
  }

  @AutoValue
  abstract static class FetchAddressUrlsTestData {
    static FetchAddressUrlsTestData create(
        Optional<ImmutableList<String>> esriFindAddressCandidatesUrls,
        Optional<String> esriFindAddressCandidatesUrl,
        Optional<String> esriArcgisApiToken,
        int expectedUrlCount) {
      return new AutoValue_RealEsriClientTest_FetchAddressUrlsTestData(
          esriFindAddressCandidatesUrls,
          esriFindAddressCandidatesUrl,
          esriArcgisApiToken,
          expectedUrlCount);
    }

    abstract Optional<ImmutableList<String>> esriFindAddressCandidatesUrls();

    abstract Optional<String> esriFindAddressCandidatesUrl();

    abstract Optional<String> esriArcgisApiToken();

    abstract int expectedUrlCount();
  }
}
