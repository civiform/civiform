package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.typesafe.config.Config;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import services.Address;
import services.AddressField;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

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
public class EsriClient implements WSBodyReadables, WSBodyWritables {
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
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
  public EsriClient(
      Config configuration,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      WSClient ws) {
    this.ws = checkNotNull(ws);
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL =
        configuration.hasPath("esri_find_address_candidates_url")
            ? Optional.of(configuration.getString("esri_find_address_candidates_url"))
            : Optional.empty();
    this.ESRI_EXTERNAL_CALL_TRIES =
        configuration.hasPath("esri_external_call_tries")
            ? configuration.getInt("esri_external_call_tries")
            : 3;
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
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

  /**
   * Calls the external Esri findAddressCandidates service to retrieve address correction
   * suggestions for the given address.
   *
   * <p>@see <a
   * href="https://developers.arcgis.com/rest/geocode/api-reference/geocoding-find-address-candidates.htm">Find
   * Address Candidates</a>
   *
   * <p>Returns an empty optional under the following conditions:
   *
   * <ul>
   *   <li>ESRI_FIND_ADDRESS_CANDIDATES_URL is not configured.
   *   <li>The external ESRI services returns a non 200 status.
   * </ul>
   *
   * If the external ESRI serice returns an error or non 200 status, then the request is retried up
   * to the configured ESRI_EXTERNAL_CALL_TRIES. If ESRI_EXTERNAL_CALL_TRIES is not configured, the
   * tries default to 3.
   */
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

  /**
   * Calls an external Esri service to get address suggestions given the provided {@link Address}.
   * Takes the returned address suggestions to build an {@link AddressSuggestionGroup}.
   *
   * @return an optional {@link AddressSuggestionGroup} if successful, or an empty optional if the
   *     request fails.
   */
  public CompletionStage<Optional<AddressSuggestionGroup>> getAddressSuggestions(Address address) {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", address.getStreet());
    addressJson.put("line2", address.getLine2());
    addressJson.put("city", address.getCity());
    addressJson.put("state", address.getState());
    addressJson.put("zip", address.getZip());

    return fetchAddressSuggestions(addressJson)
        .thenApply(
            (maybeJson) -> {
              if (maybeJson.isEmpty()) {
                logger.error(
                    "EsriClient.fetchAddressSuggestions JSON response is empty. Called by"
                        + " EsriClient.getAddressSuggestions. Address = {}",
                    address);
                return Optional.empty();
              }
              JsonNode json = maybeJson.get();
              int wkid = json.get("spatialReference").get("wkid").asInt();
              ImmutableList.Builder<AddressSuggestion> suggestionBuilder = ImmutableList.builder();
              for (JsonNode candidateJson : json.get("candidates")) {
                JsonNode location = candidateJson.get("location");
                JsonNode attributes = candidateJson.get("attributes");
                AddressLocation addressLocation =
                    AddressLocation.builder()
                        .setLongitude(location.get("x").asDouble())
                        .setLatitude(location.get("y").asDouble())
                        .setWellKnownId(wkid)
                        .build();
                Address candidateAddress =
                    Address.builder()
                        .setStreet(attributes.get("Address").asText())
                        .setLine2(attributes.get("SubAddr").asText())
                        .setCity(attributes.get("City").asText())
                        .setState(attributes.get("RegionAbbr").asText())
                        .setZip(attributes.get("Postal").asText())
                        .build();
                AddressSuggestion addressCandidate =
                    AddressSuggestion.builder()
                        .setSingleLineAddress(candidateJson.get("address").asText())
                        .setLocation(addressLocation)
                        .setScore(candidateJson.get("score").asInt())
                        .setAddress(candidateAddress)
                        .build();
                suggestionBuilder.add(addressCandidate);
              }

              AddressSuggestionGroup addressCandidates =
                  AddressSuggestionGroup.builder()
                      .setWellKnownId(wkid)
                      .setAddressSuggestions(suggestionBuilder.build())
                      .build();
              return Optional.of(addressCandidates);
            });
  }

  /**
   * Calls the external Esri query feature service layer to retrieve features of given address
   * coordinates.
   *
   * <p>@see <a
   * href="https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer-.htm">Query
   * (Feature Service/Layer)</a>
   *
   * <p>Returns an empty optional under the following conditions:
   *
   * <ul>
   *   <li>ESRI_ADDRESS_SERVICE_AREA_VALIDATION_IDS or ESRI_ADDRESS_SERVICE_AREA_VALIDATION_URLS is
   *       not configured.
   *   <li>The external ESRI services returns a non 200 status.
   * </ul>
   *
   * If the external ESRI serice returns an error or non 200 status, then the request is retried up
   * to the configured ESRI_EXTERNAL_CALL_TRIES. If ESRI_EXTERNAL_CALL_TRIES is not configured, the
   * tries default to 3.
   */
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

  /**
   * Calls an external Esri service to get the service areas of the provided {@link
   * AddressLocation}. Takes the returned service areas and returns an immutable list of {@link
   * ServiceAreaInclusion}, filtered by the services areas specified in the application config that
   * have the same {@link EsriServiceAreaValidationOption} URL.
   */
  public CompletionStage<ImmutableList<ServiceAreaInclusion>> getServiceAreaInclusionGroup(
      EsriServiceAreaValidationOption esriServiceAreaValidationOption, AddressLocation location) {
    ServiceAreaInclusion.Builder serviceAreaInclusionBuilder = ServiceAreaInclusion.builder();
    ImmutableList.Builder<ServiceAreaInclusion> inclusionListBuilder = ImmutableList.builder();

    if (!esriServiceAreaValidationConfig.isConfigurationValid()) {
      logger.error(
          "Error calling EsriClient.getServiceAreaInclusionGroups. Error:"
              + " EsriServiceAreaValidationConfig.getImmutableMap() returned empty.");
      serviceAreaInclusionBuilder
          .setServiceAreaId(esriServiceAreaValidationOption.getId())
          .setState(ServiceAreaState.FAILED)
          .setTimeStamp(Instant.now());
      inclusionListBuilder.add(serviceAreaInclusionBuilder.build());
      return CompletableFuture.completedFuture(inclusionListBuilder.build());
    }

    ImmutableList<EsriServiceAreaValidationOption> optionList =
        esriServiceAreaValidationConfig.getOptionsWithSharedBackend(
            esriServiceAreaValidationOption.getUrl());

    return fetchServiceAreaFeatures(location, esriServiceAreaValidationOption.getUrl())
        .thenApply(
            (maybeJson) -> {
              if (maybeJson.isEmpty()) {
                logger.error(
                    "EsriClient.fetchServiceAreaFeatures JSON response is empty. Called by"
                        + " EsriClient.isAddressLocationInServiceArea."
                        + " EsriServiceAreaValidationOption = {}. AddressLocation = {}",
                    esriServiceAreaValidationOption,
                    location);

                for (EsriServiceAreaValidationOption option : optionList) {
                  inclusionListBuilder.add(
                      serviceAreaInclusionBuilder
                          .setServiceAreaId(option.getId())
                          .setState(ServiceAreaState.FAILED)
                          .setTimeStamp(Instant.now())
                          .build());
                }

                return inclusionListBuilder.build();
              }

              JsonNode json = maybeJson.get();
              ReadContext ctx = JsonPath.parse(json.toString());
              List<String> features =
                  ctx.read(
                      "features[*].attributes." + esriServiceAreaValidationOption.getAttribute());

              for (EsriServiceAreaValidationOption option : optionList) {
                if (features.contains(option.getId())) {
                  inclusionListBuilder.add(
                      serviceAreaInclusionBuilder
                          .setServiceAreaId(option.getId())
                          .setState(ServiceAreaState.IN_AREA)
                          .setTimeStamp(Instant.now())
                          .build());
                } else {
                  inclusionListBuilder.add(
                      serviceAreaInclusionBuilder
                          .setServiceAreaId(option.getId())
                          .setState(ServiceAreaState.NOT_IN_AREA)
                          .setTimeStamp(Instant.now())
                          .build());
                }
              }

              return inclusionListBuilder.build();
            });
  }
}
