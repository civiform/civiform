package controllers.dev;

import static com.google.gdata.util.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.Address;
import services.geo.AddressLocation;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.geo.esri.EsriServiceAreaValidationOption;
import services.settings.SettingsManifest;
import views.PartialView;
import views.components.TextFormatter;
import views.dev.AddressCheckerPageView;
import views.dev.AddressCheckerPageViewModel;
import views.dev.hx.AddressCheckerErrorPartialViewModel;
import views.dev.hx.CorrectAddressResultsPartialViewModel;
import views.dev.hx.ServiceAreaResultsPartialViewModel;

/** Dev tools controller for test address correction and service area eligibility validation */
public final class AddressCheckerController extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(AddressCheckerController.class);

  private final SettingsManifest settingsManifest;
  private final AddressCheckerPageView addressCheckerPageView;
  private final PartialView<CorrectAddressResultsPartialViewModel> correctAddressResultsPartialView;
  private final PartialView<ServiceAreaResultsPartialViewModel> serviceAreaResultsPartialView;
  private final PartialView<AddressCheckerErrorPartialViewModel> errorPartialView;
  private final EsriClient esriClient;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private final FormFactory formFactory;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;

  @Inject
  AddressCheckerController(
      SettingsManifest settingsManifest,
      PartialView<CorrectAddressResultsPartialViewModel> correctAddressResultsPartialView,
      PartialView<ServiceAreaResultsPartialViewModel> serviceAreaResultsPartialView,
      PartialView<AddressCheckerErrorPartialViewModel> errorPartialView,
      EsriClient esriClient,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      FormFactory formFactory,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      AddressCheckerPageView addressCheckerPageView) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.correctAddressResultsPartialView = checkNotNull(correctAddressResultsPartialView);
    this.serviceAreaResultsPartialView = checkNotNull(serviceAreaResultsPartialView);
    this.errorPartialView = checkNotNull(errorPartialView);
    this.esriClient = checkNotNull(esriClient);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
    this.formFactory = checkNotNull(formFactory);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.addressCheckerPageView = checkNotNull(addressCheckerPageView);
  }

  public Result index(Http.Request request) {
    AddressCheckerPageViewModel model =
        AddressCheckerPageViewModel.builder()
            .backLinkUrl(routes.DevToolsController.index().url())
            .addressCorrectionEnabled(settingsManifest.getEsriAddressCorrectionEnabled())
            .addressValidationEnabled(
                settingsManifest.getEsriAddressServiceAreaValidationEnabled())
            .findAddressCandidatesUrls(settingsManifest.getEsriFindAddressCandidatesUrls())
            .addressServiceAreaValidationUrls(
                settingsManifest.getEsriAddressServiceAreaValidationUrls())
            .serviceAreaValidationConfigMap(serviceAreaValidationOptions())
            .correctAddressActionUrl(routes.AddressCheckerController.hxCorrectAddress().url())
            .checkServiceAreaActionUrl(routes.AddressCheckerController.hxCheckServiceArea().url())
            .build();

    return ok(addressCheckerPageView.render(request, model)).as(Http.MimeTypes.HTML);
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
          errorView(request, "An error occurred trying to correct an address"));
    }

    return esriClient
        .getAddressSuggestions(address)
        .thenApplyAsync(
            addressSuggestionGroup -> {
              ImmutableMap<String, EsriServiceAreaValidationOption> serviceAreaValidationOptions =
                  serviceAreaValidationOptions();

              CorrectAddressResultsPartialViewModel model =
                  CorrectAddressResultsPartialViewModel.builder()
                      .addressSuggestions(addressSuggestionGroup.getAddressSuggestions())
                      .serviceAreaValidationEnabled(!serviceAreaValidationOptions.isEmpty())
                      .serviceAreaValidationOptions(serviceAreaValidationOptions)
                      .checkServiceAreaActionUrl(
                          routes.AddressCheckerController.hxCheckServiceArea().url())
                      .build();

              return ok(correctAddressResultsPartialView.render(request, model))
                  .as(Http.MimeTypes.HTML);
            },
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to correct an address", ex);
              return errorView(request, "An error occurred trying to correct an address");
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
          errorView(
              request,
              "Invalid form parameters submitted when attempting service area validation"));
    }

    return esriClient
        .getServiceAreaInclusionGroup(esriServiceAreaValidationOption, addressLocation)
        .thenApplyAsync(
            serviceAreaInclusions ->
                ok(serviceAreaResultsPartialView.render(
                        request,
                        new ServiceAreaResultsPartialViewModel(
                            validationOption, serviceAreaInclusions)))
                    .as(Http.MimeTypes.HTML),
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              logger.error(ex.getMessage());

              return errorView(request, "An error occurred trying to check the service area");
            });
  }

  /**
   * Service area options to offer, or an empty map when validation is off or not configured. {@link
   * EsriServiceAreaValidationConfig#getImmutableMap()} throws when the settings are missing, so
   * only call it once validation is enabled, has URLs, and has a valid config.
   */
  private ImmutableMap<String, EsriServiceAreaValidationOption> serviceAreaValidationOptions() {
    boolean available =
        settingsManifest.getEsriAddressServiceAreaValidationEnabled()
            && settingsManifest
                .getEsriAddressServiceAreaValidationUrls()
                .map(urls -> !urls.isEmpty())
                .orElse(false)
            && esriServiceAreaValidationConfig.isConfigurationValid();

    return available ? esriServiceAreaValidationConfig.getImmutableMap() : ImmutableMap.of();
  }

  private Result errorView(Http.Request request, String message) {
    return ok(errorPartialView.render(request, new AddressCheckerErrorPartialViewModel(message)))
        .as(Http.MimeTypes.HTML);
  }
}
