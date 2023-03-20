package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

/** An abstract class for working with external Esri services. */
public abstract class EsriClient {
  final Clock clock;
  final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public EsriClient(Clock clock, EsriServiceAreaValidationConfig esriServiceAreaValidationConfig) {
    this.clock = checkNotNull(clock);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
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
  abstract CompletionStage<Optional<JsonNode>> fetchAddressSuggestions(ObjectNode addressJson);

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
                this.logger.error(
                    "RealEsriClient.fetchAddressSuggestions JSON response is empty. Called by"
                        + " RealEsriClient.getAddressSuggestions. Address = {}",
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
                      .setOriginalAddress(address)
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
  abstract CompletionStage<Optional<JsonNode>> fetchServiceAreaFeatures(
      AddressLocation location, String validationUrl);

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
          "Error calling RealEsriClient.getServiceAreaInclusionGroups. Error:"
              + " EsriServiceAreaValidationConfig.getImmutableMap() returned empty.");
      serviceAreaInclusionBuilder
          .setServiceAreaId(esriServiceAreaValidationOption.getId())
          .setState(ServiceAreaState.FAILED)
          .setTimeStamp(clock.millis());
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
                    "RealEsriClient.fetchServiceAreaFeatures JSON response is empty. Called by"
                        + " RealEsriClient.isAddressLocationInServiceArea."
                        + " EsriServiceAreaValidationOption = {}. AddressLocation = {}",
                    esriServiceAreaValidationOption,
                    location);

                for (EsriServiceAreaValidationOption option : optionList) {
                  inclusionListBuilder.add(
                      serviceAreaInclusionBuilder
                          .setServiceAreaId(option.getId())
                          .setState(ServiceAreaState.FAILED)
                          .setTimeStamp(clock.millis())
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
                          .setTimeStamp(clock.millis())
                          .build());
                } else {
                  inclusionListBuilder.add(
                      serviceAreaInclusionBuilder
                          .setServiceAreaId(option.getId())
                          .setState(ServiceAreaState.NOT_IN_AREA)
                          .setTimeStamp(clock.millis())
                          .build());
                }
              }

              return inclusionListBuilder.build();
            });
  }
}
