package services.geo.esri;
import services.geo.AddressAttributes;
import services.geo.AddressCandidates;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;

import services.geo.AddressCorrectionClient;
import services.geo.AddressLocation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;

import javax.inject.Inject;

import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.*;

public class EsriClient implements WSBodyReadables, WSBodyWritables {
  public static final String ESRI_CONTENT_TYPE = "application/json";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS = "Address, SubAddr, City, Region, Postal";
  public static final String ESRI_FIND_ADDRESS_CANDIDATES_FORMAT = "pjson";
  
  public final ConfigList esri_address_verification;
  public final String ESRI_FIND_ADDRESS_CANDIDATES_URL;
  private final WSClient ws;

  @Inject
  public EsriClient(
    Config configuration,
    WSClient ws) {
    this.esri_address_verification = configuration.getList("esri_address_verification");
    this.ESRI_FIND_ADDRESS_CANDIDATES_URL = checkNotNull(configuration).getString("esri_find_address_candidates_url");
    this.ws = ws;
  }

  public CompletionStage<JsonNode> getAddressCandidates(AddressAttributes addressAttributes) {
    WSRequest request = ws.url(this.ESRI_FIND_ADDRESS_CANDIDATES_URL);
    request.setContentType(ESRI_CONTENT_TYPE);
    request.addQueryParameter("outFields", ESRI_FIND_ADDRESS_CANDIDATES_OUT_FIELDS);
    request.addQueryParameter("f", ESRI_FIND_ADDRESS_CANDIDATES_FORMAT);
    // add addressAttributes to query
    request.addQueryParameter("address", addressAttributes.getStateValue());
    request.addQueryParameter("address2", addressAttributes.getLine2Value());
    request.addQueryParameter("city", addressAttributes.getCityValue());
    request.addQueryParameter("region", addressAttributes.getStateValue());
    request.addQueryParameter("postal", addressAttributes.getZipValue());
    CompletionStage<JsonNode> jsonFuture = request.get().thenApply(res -> res.getBody(json()));
    return jsonFuture;
  }

  public CompletionStage<JsonNode> verifyAddressCoordinates(String verificationValue, AddressLocation location) {
    HashMap<String, String> verificationConfig = new HashMap<String, String>();
    // TODO: move to private memoized function
    for (Iterator<ConfigValue> iterator = this.esri_address_verification.iterator(); iterator.hasNext();) {        
      ConfigValue configValue = (ConfigValue) iterator.next();
      /**
       * Not happy about this. There doesn't seem to be a good way to traverse or access keys on ConfigValue
       */
      String configValueStr = configValue.toString();

      Iterable<String> parsed = Splitter.on(",").split(
        configValueStr
          .replace("SimpleConfigObject({", "")
          .replace("})", ""));

      for (String el : parsed) {
        Iterable<String> keyValue = Splitter.on(":").split(el);
        String key = keyValue.iterator().next();
        String value = keyValue.iterator().next();
        if ("verification_value".equals(key) && value.equals(verificationValue)) {
          verificationConfig.put(key, value);
        }
      }
    }

    WSRequest request = ws.url(verificationConfig.get("url"));
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
