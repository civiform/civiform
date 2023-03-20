package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
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
public class RealEsriClient extends EsriClient implements WSBodyReadables, WSBodyWritables {
  private final WSClient ws;

  private static final String ESRI_CONTENT_TYPE = "application/json";
  // Specify output fields to return in the geocoding response with the outFields parameter
  private static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, RegionAbbr, Postal";
  // The service supports responses in JSON or PJSON format. You can specify the response format
  // using the f parameter. This is a required parameter
  private static final String ESRI_RESPONSE_FORMAT = "json";
  @VisibleForTesting Optional<String> ESRI_FIND_ADDRESS_CANDIDATES_URL;
  private int ESRI_EXTERNAL_CALL_TRIES;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public RealEsriClient(
      Config configuration,
      Clock clock,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      WSClient ws) {
    super(clock, esriServiceAreaValidationConfig);
    this.ws = checkNotNull(ws);
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL =
        configuration.hasPath("esri_find_address_candidates_url")
            ? Optional.of(configuration.getString("esri_find_address_candidates_url"))
            : Optional.empty();
    this.ESRI_EXTERNAL_CALL_TRIES =
        configuration.hasPath("esri_external_call_tries")
            ? configuration.getInt("esri_external_call_tries")
            : 3;
  }

  /** Retries failed requests up to the provided value */
  private CompletionStage<WSResponse> tryRequest(WSRequest request, int retries) {
    CompletionStage<WSResponse> responsePromise = request.get();
    AtomicInteger retryCount = new AtomicInteger(retries);
    responsePromise.handle(
        (result, error) -> {
          if (error != null || result.getStatus() != 200) {
            logger.error(
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
  CompletionStage<Optional<JsonNode>> fetchAddressSuggestions(ObjectNode addressJson) {
    if (this.ESRI_FIND_ADDRESS_CANDIDATES_URL.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL.get());
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    // "f" stands for "format", options are json and pjson (PrettyJson)
    request.addQueryParameter("f", ESRI_RESPONSE_FORMAT);
    // limit max locations to 3 to keep the size down, since CF stores the suggestions in the user
    // session
    request.addQueryParameter("maxLocations", "3");
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
    address.ifPresent(val -> request.addQueryParameter("address", val));
    address2.ifPresent(val -> request.addQueryParameter("address2", val));
    city.ifPresent(val -> request.addQueryParameter("city", val));
    region.ifPresent(val -> request.addQueryParameter("region", val));
    postal.ifPresent(val -> request.addQueryParameter("postal", val));

    return tryRequest(request, this.ESRI_EXTERNAL_CALL_TRIES)
        .thenApply(
            res -> {
              // return empty if still failing after retries
              if (res.getStatus() != 200) {
                return Optional.empty();
              }
              return Optional.of(res.getBody(json()));
            });
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
    request.addQueryParameter("inSR", location.getWellKnownId().toString());
    String geo = "{'x':";
    geo += location.getLongitude();
    geo += ",'y':";
    geo += location.getLatitude();
    geo += ",'spatialReference':";
    geo += location.getWellKnownId();
    request.addQueryParameter("geometry", geo);

    return tryRequest(request, this.ESRI_EXTERNAL_CALL_TRIES)
        .thenApply(
            res -> {
              // return empty if still failing after retries
              if (res.getStatus() != 200) {
                return Optional.empty();
              }
              return Optional.of(res.getBody(json()));
            });
  }
}
