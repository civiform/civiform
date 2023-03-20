package services.geo.esri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import services.AddressField;
import services.geo.AddressLocation;

/**
 * Provides overide methods for {@link EsriClient} to illiminate the need for calling the external
 * Esri services for dev and testing.
 */
public class FakeEsriClient extends EsriClient {
  private String jsonResources = System.getProperty("user.dir") + "/test/resources/esri/";
  private JsonNode addressCandidates;
  private JsonNode noAddressCandidates;
  private JsonNode serviceAreaFeatures;
  private JsonNode serviceAreaNoFeatures;
  private JsonNode serviceAreaNotInArea;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject
  public FakeEsriClient(
      Clock clock, EsriServiceAreaValidationConfig esriServiceAreaValidationConfig) {
    super(clock, esriServiceAreaValidationConfig);
  }

  /**
   * Returns address suggestions based on the provided address json.
   *
   * <p>The address key in the provided address json is used to determine which suggestions to
   * return. The other fields do not matter.
   *
   * <ul>
   *   <li>"Legit Address" – this value will provide three suggestions for tesing service area
   *       validation: "Address In Area", "Address With No Service Area Features" and "Address Not
   *       In Area"
   *   <li>"Bogus Address" – use this value for testing address that won't return any suggestions
   *   <li>"Error Address" – use this value to test what would happen if the external Esri service
   *       were to return a non 200 status. The end result is the same as "Bogus Address"
   *   <li>any other address will have the same effect as Bogus and Error
   * </ul>
   */
  @Override
  CompletionStage<Optional<JsonNode>> fetchAddressSuggestions(ObjectNode addressJson) {
    String address = addressJson.findPath(AddressField.STREET.getValue()).textValue();
    Optional<JsonNode> maybeJson = Optional.empty();
    File resource;
    FileInputStream inputStream;

    switch (address) {
      case "Legit Address":
        if (addressCandidates != null) {
          maybeJson = Optional.of(addressCandidates);
          break;
        }
        try {
          resource = new File(jsonResources + "findAddressCandidates.json");
          inputStream = new FileInputStream(resource);
          addressCandidates = Json.parse(inputStream);
          maybeJson = Optional.of(addressCandidates);
        } catch (FileNotFoundException e) {
          logger.error("fakeEsriClient fetchAddressSuggestions file not found: {}", e);
        }
        break;
      case "Bogus Address":
        if (noAddressCandidates != null) {
          maybeJson = Optional.of(noAddressCandidates);
          break;
        }

        try {
          resource = new File(jsonResources + "findAddressCandidatesNoCandidates.json");
          inputStream = new FileInputStream(resource);
          noAddressCandidates = Json.parse(inputStream);
          maybeJson = Optional.of(noAddressCandidates);
        } catch (FileNotFoundException e) {
          logger.error("fakeEsriClient fetchAddressSuggestions file not found: {}", e);
        }
        break;
      case "Error Address":
      default:
    }

    return CompletableFuture.completedFuture(maybeJson);
  }

  @Override
  CompletionStage<Optional<JsonNode>> fetchServiceAreaFeatures(
      AddressLocation location, String validationUrl) {
    String latitude = location.getLatitude().toString();
    Optional<JsonNode> maybeJson = Optional.empty();
    File resource;
    FileInputStream inputStream;

    switch (latitude) {
      case "100.0":
        if (serviceAreaFeatures != null) {
          maybeJson = Optional.of(serviceAreaFeatures);
          break;
        }
        try {
          resource = new File(jsonResources + "serviceAreaFeatures.json");
          inputStream = new FileInputStream(resource);
          serviceAreaFeatures = Json.parse(inputStream);
          maybeJson = Optional.of(serviceAreaFeatures);
        } catch (FileNotFoundException e) {
          logger.error("fakeEsriClient fetchAddressSuggestions file not found: {}", e);
        }
        break;
      case "101.0":
        if (serviceAreaNoFeatures != null) {
          maybeJson = Optional.of(serviceAreaNoFeatures);
          break;
        }
        try {
          resource = new File(jsonResources + "serviceAreaFeaturesNoFeatures.json");
          inputStream = new FileInputStream(resource);
          serviceAreaNoFeatures = Json.parse(inputStream);
          maybeJson = Optional.of(serviceAreaNoFeatures);
        } catch (FileNotFoundException e) {
          logger.error("fakeEsriClient fetchAddressSuggestions file not found: {}", e);
        }
        break;
      case "102.0":
        if (serviceAreaNotInArea != null) {
          maybeJson = Optional.of(serviceAreaNotInArea);
          break;
        }
        try {
          resource = new File(jsonResources + "serviceAreaFeaturesNotInArea.json");
          inputStream = new FileInputStream(resource);
          serviceAreaNotInArea = Json.parse(inputStream);
          maybeJson = Optional.of(serviceAreaNotInArea);
        } catch (FileNotFoundException e) {
          logger.error("fakeEsriClient fetchAddressSuggestions file not found: {}", e);
        }
        break;
      case "103.0":
      default:
    }

    return CompletableFuture.completedFuture(maybeJson);
  }
}
