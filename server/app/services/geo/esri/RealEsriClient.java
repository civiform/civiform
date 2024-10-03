package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.prometheus.client.Counter;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import services.AddressField;
import services.geo.AddressLocation;
import services.geo.esri.models.FindAddressCandidatesResponse;
import services.settings.SettingsManifest;

/**
 * Provides methods for handling reqeusts to external Esri geo and map layer services for getting
 * address suggestions and service area validation for a given address.
 *
 * <p>@see <a
 * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
 * Address Candidates</a>
 *
 * @see <a
 *     href="https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer-.htm">Query
 *     (Map Service/Layer)</a>
 */
public final class RealEsriClient extends EsriClient implements WSBodyReadables, WSBodyWritables {
  private final WSClient ws;
  private final ObjectMapper mapper = new ObjectMapper();

  private static final Counter ESRI_REQUEST_C0UNT =
      Counter.build()
          .name("esri_requests_total")
          .help("Total amount of requests to the ESRI client")
          .labelNames("status")
          .register();

  private static final Counter ESRI_USAGE_BY_ENDPOINT_COUNT =
      Counter.build()
          .name("esri_usage_by_endpoint_total")
          .help("Total calls to ESRI endpoints")
          .labelNames("uri")
          .register();

  private static final String ESRI_CONTENT_TYPE = "application/json";
  // Specify output fields to return in the geocoding response with the outFields parameter
  private static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, Region, RegionAbbr, Postal";
  // The service supports responses in JSON or PJSON format. You can specify the response format
  // using the f parameter. This is a required parameter
  private static final String ESRI_RESPONSE_FORMAT = "json";
  @VisibleForTesting ImmutableList<String> ESRI_FIND_ADDRESS_CANDIDATES_URLS;

  /**
   * The lowest score value out of 100 that will preempt loading data from another endpoint.
   * Anything below this will continue to gather data from the next available endpoint.
   */
  private static final double SCORE_THRESHOLD = 90.0;

  private int ESRI_EXTERNAL_CALL_TRIES;
  private final Optional<Integer> ESRI_WELLKNOWN_ID_OVERRIDE;
  private final Optional<String> ESRI_ARCGIS_API_TOKEN;

  private static final Logger LOGGER = LoggerFactory.getLogger(RealEsriClient.class);

  @Inject
  public RealEsriClient(
      SettingsManifest settingsManifest,
      Clock clock,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      WSClient ws) {
    super(clock, esriServiceAreaValidationConfig);
    checkNotNull(settingsManifest);
    this.ws = checkNotNull(ws);

    this.ESRI_EXTERNAL_CALL_TRIES = settingsManifest.getEsriExternalCallTries().orElse(3);
    this.ESRI_WELLKNOWN_ID_OVERRIDE = settingsManifest.getEsriWellknownIdOverride();
    this.ESRI_ARCGIS_API_TOKEN = settingsManifest.getEsriArcgisApiToken();
    this.ESRI_FIND_ADDRESS_CANDIDATES_URLS = getFindAddressCandidateUrls(settingsManifest);
  }

  /** Get the list of urls that will be used to correct addresses */
  private static ImmutableList<String> getFindAddressCandidateUrls(
      SettingsManifest settingsManifest) {
    Optional<String> esriArcgisApiToken = settingsManifest.getEsriArcgisApiToken();

    // Default to using the new setting which is a list
    ImmutableList<String> urls =
        settingsManifest.getEsriFindAddressCandidatesUrls().orElse(ImmutableList.of());

    // Fallback to using the old setting if the list is empty
    if (urls.isEmpty()
        && !settingsManifest.getEsriFindAddressCandidatesUrl().orElse("").isEmpty()) {
      urls =
          ImmutableList.<String>builder()
              .add(settingsManifest.getEsriFindAddressCandidatesUrl().get())
              .build();
    }

    if (urls.stream().anyMatch(url -> isArcGisOnlineService(url) && esriArcgisApiToken.isEmpty())) {
      LOGGER.error(
          "ArcGis Online requires an api token, but one was not configured. ArcGis Online will not"
              + " be used for address correction.");
    }

    return urls.stream()
        // Remove any arcgis.com urls if the api token is not set
        .filter(url -> !(isArcGisOnlineService(url) && esriArcgisApiToken.isEmpty()))
        .collect(ImmutableList.toImmutableList());
  }

  /** Retries failed requests up to the provided value */
  private CompletionStage<WSResponse> tryRequest(WSRequest request, int retries) {
    CompletionStage<WSResponse> responsePromise = request.get();
    AtomicInteger retryCount = new AtomicInteger(retries);
    responsePromise.handle(
        (result, error) -> {
          if (error != null || result.getStatus() != 200) {
            LOGGER.error(
                "Esri API error: {}", error != null ? error.toString() : result.getStatusText());
            if (retries > 0) {
              return tryRequest(request, retryCount.decrementAndGet());
            } else {
              return responsePromise;
            }
          } else {
            return CompletableFuture.completedFuture(result);
          }
        });

    return responsePromise;
  }

  @Override
  @VisibleForTesting
  CompletionStage<Optional<FindAddressCandidatesResponse>> fetchAddressSuggestions(
      ObjectNode addressJson) {
    return processAddressSuggestionUrlsSequentially(
        addressJson, ESRI_FIND_ADDRESS_CANDIDATES_URLS, Optional.empty());
  }

  /**
   * Recursive method to get address correction data from one or more findAddressCandidates
   * endpoints. This will stop when there are no urls in the list to process or we end early because
   * we have results that are greater than the scoring threshold.
   *
   * @param addressJson Uncorrected address
   * @param urls A list of urls that will be processed in order; may stop early if surpassing score
   *     threshold. The first url in the list will be fetched. Recursive calls will always send in
   *     all elements except the first one. Processing ends when there are no urls in the list.
   * @param optionalPreviousRootNode The response from the previous findAddressCandidates call, if
   *     any
   * @return The CompletionStage for result of findAddressCandidates, if any. The `candidates` array
   *     will be the merged results of multiple endpoints if more than one is called.
   */
  private CompletionStage<Optional<FindAddressCandidatesResponse>>
      processAddressSuggestionUrlsSequentially(
          ObjectNode addressJson,
          ImmutableList<String> urls,
          Optional<FindAddressCandidatesResponse> optionalPreviousRootNode) {
    // Base case: All URLs processed or no valid URLs left
    if (urls.isEmpty()) {
      return CompletableFuture.completedFuture(optionalPreviousRootNode);
    }

    // Urls will have at least one item in the list at this point
    var request = createWebRequest(urls.stream().findFirst().get(), addressJson);

    // Perform the request and handle response
    return tryRequest(request, this.ESRI_EXTERNAL_CALL_TRIES)
        .thenCompose(
            wsResponse -> {
              ESRI_REQUEST_C0UNT.labels(String.valueOf(wsResponse.getStatus())).inc();
              incrementEsriEndpointUsageCounter(wsResponse.getUri());

              // Skip the first url which we've just called, send the rest to the next recursive
              // call
              var nextSetOfUrls = urls.stream().skip(1).collect(ImmutableList.toImmutableList());

              if (wsResponse.getStatus() != 200) {
                // If request fails, proceed to the next URL
                return processAddressSuggestionUrlsSequentially(
                    addressJson, nextSetOfUrls, Optional.empty());
              } else {
                // Process the successful response
                JsonNode rootNode = wsResponse.asJson();

                FindAddressCandidatesResponse response;
                try {
                  response =
                      mapper.readValue(rootNode.toString(), FindAddressCandidatesResponse.class);
                } catch (JsonProcessingException e) {
                  LOGGER.error("Unable to parse JSON from wsResponse", e);
                  return processAddressSuggestionUrlsSequentially(
                      addressJson, nextSetOfUrls, Optional.empty());
                }

                // If we have data from a previous call we'll stitch the candidate records together
                if (optionalPreviousRootNode.isPresent()) {
                  response.addCandidates(optionalPreviousRootNode.get().candidates());
                }

                // This will check that all results are under the score threshold.
                //
                // The score threshold is checking to see that we've gotten enough results with a
                // high enough score to warrant not need to check any other endpoints for results.
                //
                // Reason for doing this is to not make more external calls than are needed. This
                // is both for performance and billing reasons.
                boolean hasAnyNodesUnderTheScoreThreshold =
                    response.candidates().stream()
                        .anyMatch(candidate -> candidate.score() < SCORE_THRESHOLD);

                // If there are no results from this url we definitely want to check the next
                // available url to see if there are any there.
                boolean hasNoResults = response.candidates().isEmpty();

                boolean loadAnotherUrl = hasAnyNodesUnderTheScoreThreshold || hasNoResults;

                if (loadAnotherUrl == true) {
                  return processAddressSuggestionUrlsSequentially(
                      addressJson, nextSetOfUrls, Optional.of(response));
                }

                return CompletableFuture.completedFuture(Optional.of(response));
              }
            });
  }

  /**
   * Build the request payload to be sent to the Esri service
   *
   * @param url The full url to the fetchAddressSuggestions endpoint
   * @param addressJson Uncorrected address
   * @return Play WSRequest object ready to be sent to the target endpoint
   */
  private WSRequest createWebRequest(String url, ObjectNode addressJson) {
    WSRequest request = ws.url(url);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    // "f" stands for "format", options are json and pjson (PrettyJson)
    request.addQueryParameter("f", ESRI_RESPONSE_FORMAT);

    // Override the spatial reference if provided
    ESRI_WELLKNOWN_ID_OVERRIDE.ifPresent(val -> request.addQueryParameter("outSR", val.toString()));

    // limit max locations to 3 to keep the size down, since CF stores the suggestions in the user
    // session
    request.addQueryParameter("maxLocations", "3");

    // The forStorage parameter specifies whether the results of the operation will be persisted.
    // The default value is false, which indicates the results of the operation can't be stored, but
    // they can be temporarily displayed on a map, for instance. If you store the results, in a
    // database, for example, you need to set this parameter to true.
    request.addQueryParameter("forStorage", "true");

    Optional<String> address =
        Optional.ofNullable(addressJson.findPath(AddressField.STREET.getValue()).textValue());
    Optional<String> address2 =
        Optional.ofNullable(addressJson.findPath(AddressField.LINE2.getValue()).textValue());
    Optional<String> city =
        Optional.ofNullable(addressJson.findPath(AddressField.CITY.getValue()).textValue());
    Optional<String> region =
        Optional.ofNullable(addressJson.findPath(AddressField.STATE.getValue()).textValue());
    Optional<String> postal =
        Optional.ofNullable(addressJson.findPath(AddressField.ZIP.getValue()).textValue());

    if (isArcGisOnlineService(url)) {
      ESRI_ARCGIS_API_TOKEN.ifPresent(val -> request.addQueryParameter("token", val));
    }

    address.ifPresent(val -> request.addQueryParameter("address", val));
    address2.ifPresent(val -> request.addQueryParameter("address2", val));
    city.ifPresent(val -> request.addQueryParameter("city", val));
    region.ifPresent(val -> request.addQueryParameter("region", val));
    postal.ifPresent(val -> request.addQueryParameter("postal", val));

    return request;
  }

  /**
   * Check if this url is for Esri's ArcGIS Online service
   *
   * @param url endpoint used to call an esri service
   * @return True if the url is for an ArcGIS Online service, otherwise False
   */
  private static boolean isArcGisOnlineService(String url) {
    return url.contains("arcgis.com");
  }

  @Override
  @VisibleForTesting
  CompletionStage<Optional<JsonNode>> fetchServiceAreaFeatures(
      AddressLocation location, String validationUrl) {
    WSRequest request = ws.url(validationUrl);
    request.setContentType(ESRI_CONTENT_TYPE);
    // "f" stands for "format", options are json and pjson (PrettyJson)
    request.addQueryParameter("f", ESRI_RESPONSE_FORMAT);
    request.addQueryParameter("geometryType", "esriGeometryPoint");
    request.addQueryParameter("returnGeometry", "false");
    request.addQueryParameter("outFields", "*");
    // "inSR" specifies the spatial reference for the service to use
    request.addQueryParameter("inSR", Integer.toString(location.getWellKnownId()));
    String geo = "{'x':";
    geo += location.getLongitude();
    geo += ",'y':";
    geo += location.getLatitude();
    geo += ",'spatialReference':";
    geo += location.getWellKnownId();
    geo += "}";

    request.addQueryParameter("geometry", geo);

    return tryRequest(request, this.ESRI_EXTERNAL_CALL_TRIES)
        .thenApply(
            res -> {
              incrementEsriEndpointUsageCounter(res.getUri());

              // return empty if still failing after retries
              if (res.getStatus() != 200) {
                return Optional.empty();
              }
              return Optional.of(res.getBody(json()));
            });
  }

  /** Increment the counter for esri endpoint usage */
  private void incrementEsriEndpointUsageCounter(URI uri) {
    // Ignores query parameters and fragments.
    String endpointUrl = String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPath());
    ESRI_USAGE_BY_ENDPOINT_COUNT.labels(endpointUrl).inc();
  }
}
