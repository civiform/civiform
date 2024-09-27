package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
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
import services.geo.esri.models.Attributes;
import services.geo.esri.models.Candidate;
import services.geo.esri.models.FindAddressCandidatesResponse;

/** An abstract class for working with external Esri services. */
public abstract class EsriClient {
  final Clock clock;
  final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ObjectMapper mapper = new ObjectMapper();

  private static final Histogram ESRI_LOOKUP_TIME =
      Histogram.build()
          .name("esri_lookup_time_seconds")
          .help("Execution time of ESRI lookup")
          .register();

  private static final Counter ESRI_LOOKUP_COUNT =
      Counter.build()
          .name("esri_lookup_total")
          .help("Values retrieved in ESRI lookup")
          .labelNames("type")
          .register();

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
   * @return an {@link AddressSuggestionGroup}, which may have an empty list of suggestions if the
   *     Esri service had an error or returned bad data.
   */
  public CompletionStage<AddressSuggestionGroup> getAddressSuggestions(Address address) {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", address.getStreet());
    addressJson.put("line2", address.getLine2());
    addressJson.put("city", address.getCity());
    addressJson.put("state", address.getState());
    addressJson.put("zip", address.getZip());

    Histogram.Timer timer = ESRI_LOOKUP_TIME.startTimer();
    return fetchAddressSuggestions(addressJson)
        .thenApply(
            (maybeJson) -> {
              if (maybeJson.isEmpty()) {
                logger.error(
                    "EsriClient.fetchAddressSuggestions JSON response is empty. Called by"
                        + " EsriClient.getAddressSuggestions. Address = {}",
                    address);
                ESRI_LOOKUP_COUNT.labels("No suggestions").inc();
                return AddressSuggestionGroup.empty(address);
              }

              try {
                FindAddressCandidatesResponse response =
                    mapper.readValue(
                        maybeJson.get().toString(), FindAddressCandidatesResponse.class);

                if (response.spatialReference().isEmpty()) {
                  return AddressSuggestionGroup.empty(address);
                }

                ImmutableList.Builder<AddressSuggestion> suggestionBuilder =
                    ImmutableList.builder();

                int wkid = response.spatialReference().get().wkid();

                for (Candidate candidate : response.candidates()) {
                  AddressLocation addressLocation =
                      AddressLocation.builder()
                          .setLongitude(candidate.location().x())
                          .setLatitude(candidate.location().y())
                          .setWellKnownId(wkid)
                          .build();

                  Address candidateAddress =
                      mapAddressAttributesJson(candidate.attributes(), address);

                  if (candidateAddress.getStreet().isEmpty()
                      || candidateAddress.getCity().isEmpty()
                      || candidateAddress.getState().isEmpty()
                      || candidateAddress.getZip().isEmpty()) {
                    ESRI_LOOKUP_COUNT.labels("Partially formed address").inc();
                    continue;
                  }

                  AddressSuggestion addressCandidate =
                      AddressSuggestion.builder()
                          .setSingleLineAddress(candidate.address())
                          .setLocation(addressLocation)
                          .setScore(candidate.score())
                          .setAddress(candidateAddress)
                          .build();

                  ESRI_LOOKUP_COUNT.labels("Full address").inc();
                  suggestionBuilder.add(addressCandidate);
                }

                AddressSuggestionGroup addressCandidates =
                    AddressSuggestionGroup.builder()
                        .setWellKnownId(wkid)
                        .setAddressSuggestions(suggestionBuilder.build())
                        .setOriginalAddress(address)
                        .build();

                // Record the execution time of the esri lookup process.
                timer.observeDuration();
                return addressCandidates;
              } catch (JsonProcessingException e) {
                logger.error(
                    "EsriClient.fetchAddressSuggestions could not parse JSON response. Called by"
                        + " EsriClient.getAddressSuggestions. Address = {}",
                    address);
                ESRI_LOOKUP_COUNT.labels("No suggestions").inc();
                return AddressSuggestionGroup.empty(address);
              }
            });
  }

  static Address mapAddressAttributesJson(Attributes attributes, Address address) {
    return Address.builder()
        .setStreet(attributes.address())
        // If there is no value in SubAddr return the value the user entered. It may not be
        // returned by the ESRI instance and we don't want to lose the value.
        .setLine2(
            attributes.subAddr().isPresent() ? attributes.subAddr().get() : address.getLine2())
        .setCity(attributes.city())
        .setState(getStateAbbreviationFromAttributes(attributes, address))
        .setZip(attributes.postal())
        .build();
  }

  /**
   * Get the state abbreviation. If the value from ESRI is not the two character state abbreviation,
   * fallback to using the state that was originally set by the user.
   */
  private static String getStateAbbreviationFromAttributes(Attributes attributes, Address address) {
    String result = attributes.stateAbbreviation();

    if (result.length() != 2) {
      result = address.getState();
    }

    return result;
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
          "Error calling EsriClient.getServiceAreaInclusionGroups. Error:"
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
                          .setTimeStamp(clock.millis())
                          .build());
                }

                return inclusionListBuilder.build();
              }

              JsonNode json = maybeJson.get();
              ReadContext ctx = JsonPath.parse(json.toString());

              // Earlier this was just using the results of read directly. While the list
              // was explicitly typed for String it still had some values as non-strings
              // which was preventing the later contains check from correctly matching.
              // This creates a copy of the results because trying to directly stream
              // from it results in json array errors.
              ImmutableList<String> features =
                  List.copyOf(
                          ctx.read(
                              "features[*].attributes."
                                  + esriServiceAreaValidationOption.getAttribute()))
                      .stream()
                      .map(Object::toString)
                      .collect(ImmutableList.toImmutableList());

              for (EsriServiceAreaValidationOption option : optionList) {
                inclusionListBuilder.add(
                    serviceAreaInclusionBuilder
                        .setServiceAreaId(option.getId())
                        .setState(
                            features.contains(option.getId())
                                ? ServiceAreaState.IN_AREA
                                : ServiceAreaState.NOT_IN_AREA)
                        .setTimeStamp(clock.millis())
                        .build());
              }

              return inclusionListBuilder.build();
            });
  }
}
