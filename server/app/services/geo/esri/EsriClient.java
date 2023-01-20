package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ObjectInputFilter.Config;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;

/**
 * Provides methods for handling reqeusts to Esri geo services
 * 
 * <p> @see a href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Find_Address_Candidates/02ss00000015000000/">Find Address Candidates</a>
 * 
 * @see a href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Query_Map_Service_Layer/02ss0000000r000000/">Query (Map Service/Layer)</a>
 */
public class EsriClient implements WSBodyReadables, WSBodyWritables {
  private final WSClient ws;

  public static final String ESRI_CONTENT_TYPE = "application/json";
  // Out Fields are passed to Esri geo services to indicate which fields should return in the response
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, Region, Postal";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "json";
  public final String ESRI_FIND_ADDRESS_CANDIDATES_URL;

  @Inject
  public EsriClient(Config configuration, WSClient ws) {
    this.ws = ws;
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL =
        checkNotNull(configuration).getString("esri_find_address_candidates_url");
  }

  /**
   * Returns address candidates from Esri's findAddressCandidates service
   * 
   * <p> @see <a href="https://gisdata.seattle.gov/server/sdk/rest/index.html#/Find_Address_Candidates/02ss00000015000000/">Find Address Candidates</a>
   */
  public CompletionStage<JsonNode> fetchAddressSuggestions(ObjectNode addressJson) {
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    // "f" stands for "format", options are json and pjson (PrettyJson)
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    String address = addressJson.findPath("street").textValue();
    String address2 = addressJson.findPath("line2").textValue();
    String city = addressJson.findPath("city").textValue();
    String region = addressJson.findPath("state").textValue();
    String postal = addressJson.findPath("zip").textValue();
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
    CompletionStage<JsonNode> jsonFuture = request.get().thenApply(res -> res.getBody(json()));
    return jsonFuture;
  }

  /**
   * Returns an {@link AddressSuggestionGroup} future and is the primary way CiviForm services should interact with the Esri API
   */
  public CompletionStage<AddressSuggestionGroup> getAddressSuggestionGroup(Address address) {
    ObjectNode addressJson = Json.newObject();
    addressJson.put("street", address.getStreet());
    addressJson.put("line2", address.getLine2());
    addressJson.put("city", address.getCity());
    addressJson.put("state", address.getState());
    addressJson.put("zip", address.getZip());

    CompletionStage<JsonNode> addressSuggestionGroupFuture = fetchAddressSuggestions(addressJson);
    return addressSuggestionGroupFuture.thenApply(
        (JsonNode json) -> {
          int wkid = json.get("spatialReference").get("wkid").asInt();
          List<AddressSuggestion> candidates = new ArrayList<>();
          for (Iterator<JsonNode> iterator = json.get("candidates").iterator();
              iterator.hasNext(); ) {
            JsonNode candidateJson = (JsonNode) iterator.next();
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
            candidates.add(addressCandidate);
          }

          AddressSuggestionGroup addressCandidates =
              AddressSuggestionGroup.builder()
                  .setWellKnownId(wkid)
                  .setAddressSuggestions(ImmutableList.copyOf(candidates))
                  .build();
          return addressCandidates;
        });
  }
}
