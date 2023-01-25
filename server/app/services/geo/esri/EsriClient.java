package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.mutable.MutableInt;
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
  private final WSClient ws;

  public static final String ESRI_CONTENT_TYPE = "application/json";
  // Specify output fields to return in the geocoding response with the outFields parameter
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, Region, Postal";
  // The service supports responses in JSON or PJSON format. You can specify the response format
  // using the f parameter. This is a required parameter
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "json";
  public Optional<String> ESRI_FIND_ADDRESS_CANDIDATES_URL;
  public int ESRI_FIND_ADDRESS_CANDIDATES_TRIES;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public EsriClient(Config configuration, WSClient ws) {
    this.ws = checkNotNull(ws);
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL = configuration.hasPath("esri_find_address_candidates_url") ? Optional.of(configuration.getString("esri_find_address_candidates_url")) : Optional.empty();
    this.ESRI_FIND_ADDRESS_CANDIDATES_TRIES = configuration.hasPath("esri_find_address_candidates_tries") ? configuration.getInt("esri_find_address_candidates_tries") : 3;
  }

  /** Retries failed requests up to the provided value */
  private CompletionStage<WSResponse> tryRequest(WSRequest request, MutableInt retries) {
    CompletionStage<WSResponse> responsePromise = request.get();
    responsePromise.handle(
        (result, error) -> {
          if (error != null || result.getStatus() != 200) {
            logger.error(
                "Esri API error: {}", error != null ? error.toString() : result.getStatusText());
            if (retries.compareTo(new MutableInt(0)) > 0) {
              retries.decrement();
              return tryRequest(request, retries);
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
   * href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Find_Address_Candidates/02ss00000015000000/">Find
   * Address Candidates</a>
   *
   * <p>Returns an empty optional under the following conditions:
   *
   * <ul>
   *   <li>ESRI_FIND_ADDRESS_CANDIDATES_URL is not configured.
   *   <li>The external ESRI service returns an error.
   *   <li>The external ESRI services returns a non 200 status.
   * </ul>
   *
   * If the external ESRI serice returns an error or non 200 status, then the request is retried up
   * to the configured ESRI_FIND_ADDRESS_CANDIDATES_TRIES. If ESRI_FIND_ADDRESS_CANDIDATES_TRIES is
   * not configured, the tries default to 3.
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
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
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

    MutableInt tries = new MutableInt(this.ESRI_FIND_ADDRESS_CANDIDATES_TRIES);
    return tryRequest(request, tries)
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
   * @return an optional {@link AddressSuggestionGroup} if successful, or an empty optional if the request fails.
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
                        .setLongitude(location.get("x").asLong())
                        .setLatitude(location.get("y").asLong())
                        .setWellKnownId(wkid)
                        .build();
                Address candidateAddress =
                    Address.builder()
                        .setStreet(attributes.get("Address").toString())
                        .setLine2(attributes.get("SubAddr").toString())
                        .setCity(attributes.get("City").toString())
                        .setState(attributes.get("Region").toString())
                        .setZip(attributes.get("Postal").toString())
                        .build();
                AddressSuggestion addressCandidate =
                    AddressSuggestion.builder()
                        .setSingleLineAddress(candidateJson.get("address").toString())
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
}
