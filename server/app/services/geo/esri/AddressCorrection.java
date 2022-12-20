package services.geo.esri;

import static com.google.common.base.Preconditions.checkNotNull;

import services.geo.AddressCorrectionClient;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.libs.ws.*;

@Singleton
class AddressCorrection implements WSBodyReadables, WSBodyWritables {
  public static final String ESRI_CONTENT_TYPE = "application/json";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_URL = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS = "Address, SubAddr, City, Region, Postal";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "pjson";

  private final WSClient ws;

  @Inject;
  public AddressCorrection(WSClient ws) {
    this.ws = ws;
  }

  @Override
  public CompletetionStage<AddressCandidates> getAddressCandidates(String address) {
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("singleLine", address);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    // ...
  }
}
