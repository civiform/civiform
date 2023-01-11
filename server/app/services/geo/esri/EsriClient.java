package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import play.libs.ws.*;
import services.geo.AddressAttributes;
import services.geo.AddressLocation;

public class EsriClient implements WSBodyReadables, WSBodyWritables {
  public static final String ESRI_CONTENT_TYPE = "application/json";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS =
      "Address, SubAddr, City, Region, Postal";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "pjson";

  public final String ESRI_FIND_ADDRESS_CANDIDATES_URL;
  public final ConfigList ESRI_ADDRESS_VERIFICATION_LABELS;
  public final ConfigList ESRI_ADDRESS_VERIFICATION_VALUES;
  public final ConfigList ESRI_ADDRESS_VERIFICATION_URLS;
  public final ConfigList ESRI_ADDRESS_VERIFICATION_PATHS;

  private final WSClient ws;

  @Inject
  public EsriClient(Config configuration, WSClient ws) {
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL =
        checkNotNull(configuration).getString("esri_find_address_candidates_url");
    this.ESRI_ADDRESS_VERIFICATION_LABELS =
        checkNotNull(configuration).getList("esri_address_verification_labels");
    this.ESRI_ADDRESS_VERIFICATION_VALUES =
        checkNotNull(configuration).getList("esri_address_verification_values");
    this.ESRI_ADDRESS_VERIFICATION_URLS =
        checkNotNull(configuration).getList("esri_address_verification_urls");
    this.ESRI_ADDRESS_VERIFICATION_PATHS =
        checkNotNull(configuration).getList("esri_address_verification_paths");
    this.ws = ws;
  }

  public CompletionStage<JsonNode> getAddressCandidates(AddressAttributes addressAttributes) {
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    // add addressAttributes to query
    request.addQueryParameter("address", addressAttributes.getStreetValue());
    request.addQueryParameter("address2", addressAttributes.getLine2Value());
    request.addQueryParameter("city", addressAttributes.getCityValue());
    request.addQueryParameter("region", addressAttributes.getStateValue());
    request.addQueryParameter("postal", addressAttributes.getZipValue());
    CompletionStage<JsonNode> jsonFuture = request.get().thenApply(res -> res.getBody(json()));
    return jsonFuture;
  }

  public CompletionStage<JsonNode> verifyAddressCoordinates(
      String verificationValue, AddressLocation location) {
    List<String> verificationValues =
        this.ESRI_ADDRESS_VERIFICATION_VALUES.stream()
            .map(configValue -> (String) configValue.unwrapped())
            .collect(Collectors.toList());

    int verificationIndex = verificationValues.indexOf(verificationValue);

    String verificationUrl =
        this.ESRI_ADDRESS_VERIFICATION_URLS.get(verificationIndex).unwrapped().toString();

    WSRequest request = ws.url(verificationUrl);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    request.addQueryParameter("geometryType", "esriGeometryPoint");
    request.addQueryParameter("outFields", "*");
    String geo = "{'x':";
    geo += location.getX();
    geo += ",'y':";
    geo += location.getY();
    geo += ",'spatialReference':";
    geo += location.getWkid();
    request.addQueryParameter("geometry", geo);

    CompletionStage<JsonNode> jsonFuture = request.get().thenApply(res -> res.getBody(json()));
    return jsonFuture;
  }
}
