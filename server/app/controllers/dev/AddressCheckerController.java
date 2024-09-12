package controllers.dev;

import static com.google.gdata.util.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.h2;

import com.google.inject.Inject;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.Address;
import services.geo.AddressLocation;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.geo.esri.EsriServiceAreaValidationOption;
import services.settings.SettingsManifest;
import views.components.TextFormatter;
import views.dev.AddressCheckerView;
import views.dev.hx.CorrectAddressViewPartial;
import views.dev.hx.ServiceAreaCheckViewPartial;

/** Dev tools controller for test address correction and service area eligibility validation */
public final class AddressCheckerController extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(AddressCheckerController.class);

  private final SettingsManifest settingsManifest;
  private final AddressCheckerView addressCheckerView;
  private final CorrectAddressViewPartial correctAddressViewPartial;
  private final ServiceAreaCheckViewPartial checkServiceAreaViewPartial;
  private final EsriClient esriClient;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private final FormFactory formFactory;

  @Inject
  AddressCheckerController(
      SettingsManifest settingsManifest,
      AddressCheckerView addressCheckerView,
      CorrectAddressViewPartial correctAddressViewPartial,
      ServiceAreaCheckViewPartial checkServiceAreaViewPartial,
      EsriClient esriClient,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      FormFactory formFactory) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.addressCheckerView = checkNotNull(addressCheckerView);
    this.correctAddressViewPartial = checkNotNull(correctAddressViewPartial);
    this.checkServiceAreaViewPartial = checkServiceAreaViewPartial;
    this.esriClient = checkNotNull(esriClient);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
    this.formFactory = checkNotNull(formFactory);
  }

  public Result index(Http.Request request) {
    return ok(addressCheckerView.render(request));
  }

  /** Performs address correction and returns the results */
  public CompletionStage<Result> hxCorrectAddress(Http.Request request) {
    Address address;

    try {
      if (!Objects.equals(request.method(), "POST")) {
        throw new RuntimeException(
            String.format(
                "Request method was expected to be a POST, but was %s", request.method()));
      }

      DynamicForm formData = formFactory.form().bindFromRequest(request);

      address =
          Address.builder()
              .setStreet(TextFormatter.sanitizeHtml(formData.get("address1")))
              .setLine2(TextFormatter.sanitizeHtml(formData.get("address2")))
              .setCity(TextFormatter.sanitizeHtml(formData.get("city")))
              .setState(TextFormatter.sanitizeHtml(formData.get("state")))
              .setZip(TextFormatter.sanitizeHtml(formData.get("zip")))
              .build();
    } catch (RuntimeException ex) {
      logger.error("An error occurred trying to correct an address", ex);
      return CompletableFuture.completedFuture(
          ok(
              h2("An error occurred trying to correct an address")
                  .withClass("text-red-500")
                  .toString()));
    }

    return esriClient
        .getAddressSuggestions(address)
        .thenApply(
            addressSuggestionGroup ->
                ok(
                    correctAddressViewPartial
                        .render(
                            request,
                            settingsManifest,
                            addressSuggestionGroup,
                            esriServiceAreaValidationConfig)
                        .toString()))
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to correct an address", ex);
              return ok(
                  h2("An error occurred trying to correct an address")
                      .withClass("text-red-500")
                      .toString());
            });
  }

  /** Performs service area validation check and returns the results */
  public CompletionStage<Result> hxCheckServiceArea(Http.Request request) {
    EsriServiceAreaValidationOption esriServiceAreaValidationOption;
    AddressLocation addressLocation;
    String validationOption;

    try {
      if (!Objects.equals(request.method(), "POST")) {
        throw new RuntimeException(
            String.format(
                "Request method was expected to be a POST, but was %s", request.method()));
      }

      DynamicForm formData = formFactory.form().bindFromRequest(request);

      validationOption = TextFormatter.sanitizeHtml(formData.get("validationOption"));

      esriServiceAreaValidationOption =
          esriServiceAreaValidationConfig.getOptionByServiceAreaId(validationOption).get();

      addressLocation =
          AddressLocation.builder()
              .setLatitude(Double.parseDouble(formData.get("latitude")))
              .setLongitude(Double.parseDouble(formData.get("longitude")))
              .setWellKnownId(Integer.parseInt(formData.get("wellKnownId")))
              .build();
    } catch (RuntimeException ex) {
      logger.error(ex.getMessage());

      return CompletableFuture.completedFuture(
          ok(
              h2("Invalid form parameters submitted when attempting service area validation")
                  .withClass("text-red-500")
                  .toString()));
    }

    return esriClient
        .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, addressLocation)
        .thenApply(
            serviceAreaInclusions ->
                ok(
                    checkServiceAreaViewPartial
                        .render(validationOption, serviceAreaInclusions)
                        .toString()))
        .exceptionally(
            ex -> {
              logger.error(ex.getMessage());

              return ok(
                  h2("An error occurred trying to correct an address")
                      .withClass("text-red-500")
                      .toString());
            });
  }
}
