package services.geo.esri;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.mutable.MutableInt;
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
 * Provides methods for handling reqeusts to Esri geo services
 *
 * <p>@see a
 * href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Find_Address_Candidates/02ss00000015000000/">Find
 * Address Candidates</a>
 *
 * @see a
 *     href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Query_Map_Service_Layer/02ss0000000r000000/">Query
 *     (Map Service/Layer)</a>
 */
public class EsriClient implements WSBodyReadables, WSBodyWritables {
  private final WSClient ws;

  public static final String ESRI_CONTENT_TYPE = "application/json";
  // Out Fields are passed to Esri geo services to indicate which fields should return in the
  // response
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, Region, Postal";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "json";
  public Optional<String> ESRI_FIND_ADDRESS_CANDIDATES_URL;

  @Inject
  public EsriClient(Config configuration, WSClient ws) {
    this.ws = ws;
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL =
        Optional.ofNullable(configuration.getString("esri_find_address_candidates_url"));
  }

  /** Retries failed requests up to the provided value */
  private CompletionStage<WSResponse> tryRequest(WSRequest request, MutableInt retries) {
    CompletionStage<WSResponse> responsePromise = request.get();
    responsePromise.handle(
        (result, error) -> {
          if (error != null || result.getStatus() != 200) {
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
   * Returns address candidates from Esri's findAddressCandidates service
   *
   * <p>@see <a
   * href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Find_Address_Candidates/02ss00000015000000/">Find
   * Address Candidates</a>
   */
  public CompletionStage<Optional<JsonNode>> fetchAddressSuggestions(ObjectNode addressJson) {
    if (this.ESRI_FIND_ADDRESS_CANDIDATES_URL.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL.get());
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    // "f" stands for "format", options are json and pjson (PrettyJson)
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    String address = addressJson.findPath(AddressField.STREET.getValue()).textValue();
    System.out.println(address);
    String address2 = addressJson.findPath(AddressField.LINE2.getValue()).textValue();
    String city = addressJson.findPath(AddressField.CITY.getValue()).textValue();
    String region = addressJson.findPath(AddressField.STATE.getValue()).textValue();
    String postal = addressJson.findPath(AddressField.ZIP.getValue()).textValue();
    if (address != null) {
      request.addQueryParameter("address", address);
    }
    if (address2 != null) {
      request.addQueryParameter("address2", address2);
    }
    if (city != null) {
      request.addQueryParameter("city", city);
    }
    if (region != null) {
      request.addQueryParameter("region", region);
    }
    if (postal != null) {
      request.addQueryParameter("postal", postal);
    }
    MutableInt tries = new MutableInt(3);
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
   * Returns an {@link AddressSuggestionGroup} future and is the primary way CiviForm services
   * should interact with the Esri API
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
