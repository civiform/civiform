package controllers.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import services.Address;
import services.geo.AddressLocation;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.settings.SettingsManifest;
import views.PartialView;
import views.dev.AddressCheckerPageView;
import views.dev.hx.AddressCheckerErrorPartialViewModel;
import views.dev.hx.CorrectAddressResultsPartialViewModel;
import views.dev.hx.ServiceAreaResultsPartialViewModel;

public class AddressCheckerControllerTest extends ResetPostgres {

  private SettingsManifest settingsManifest;
  private EsriClient esriClient;
  private AddressCheckerController controller;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    esriClient = mock(EsriClient.class);

    when(settingsManifest.getEsriAddressCorrectionEnabled()).thenReturn(true);
    when(settingsManifest.getEsriFindAddressCandidatesUrls())
        .thenReturn(Optional.of(ImmutableList.of("http://esri/findAddressCandidates")));

    Injector injector = instanceOf(Injector.class);

    // No service area settings at all, as in a deployment that only uses address correction
    controller =
        new AddressCheckerController(
            settingsManifest,
            injector.getInstance(
                Key.get(new TypeLiteral<PartialView<CorrectAddressResultsPartialViewModel>>() {})),
            injector.getInstance(
                Key.get(new TypeLiteral<PartialView<ServiceAreaResultsPartialViewModel>>() {})),
            injector.getInstance(
                Key.get(new TypeLiteral<PartialView<AddressCheckerErrorPartialViewModel>>() {})),
            esriClient,
            new EsriServiceAreaValidationConfig(ConfigFactory.empty()),
            instanceOf(FormFactory.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(AddressCheckerPageView.class));
  }

  @Test
  public void index_validationDisabledAndUnconfigured_rendersCorrectionForm() {
    when(settingsManifest.getEsriAddressServiceAreaValidationEnabled()).thenReturn(false);
    when(settingsManifest.getEsriAddressServiceAreaValidationUrls()).thenReturn(Optional.empty());

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("Correct Address")
        .contains("Cannot show service area validation tool");
  }

  @Test
  public void index_validationEnabledButConfigMissing_rendersPage() {
    when(settingsManifest.getEsriAddressServiceAreaValidationEnabled()).thenReturn(true);
    when(settingsManifest.getEsriAddressServiceAreaValidationUrls())
        .thenReturn(Optional.of(ImmutableList.of("http://esri/serviceArea")));

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Correct Address");
  }

  @Test
  public void hxCorrectAddress_validationUnconfigured_rendersResultsWithoutValidation() {
    when(settingsManifest.getEsriAddressServiceAreaValidationEnabled()).thenReturn(false);
    when(settingsManifest.getEsriAddressServiceAreaValidationUrls()).thenReturn(Optional.empty());

    Address address =
        Address.builder()
            .setStreet("Address In Area")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();
    AddressSuggestion suggestion =
        AddressSuggestion.builder()
            .setSingleLineAddress("Address In Area, Redlands, CA, 92373")
            .setLocation(
                AddressLocation.builder()
                    .setLatitude(100)
                    .setLongitude(-100)
                    .setWellKnownId(4326)
                    .build())
            .setScore(100)
            .setAddress(address)
            .build();
    when(esriClient.getAddressSuggestions(any()))
        .thenReturn(
            CompletableFuture.completedFuture(
                AddressSuggestionGroup.builder()
                    .setWellKnownId(4326)
                    .setOriginalAddress(address)
                    .setAddressSuggestions(ImmutableList.of(suggestion))
                    .build()));

    Http.Request request =
        fakeRequestBuilder()
            .call(routes.AddressCheckerController.hxCorrectAddress())
            .bodyForm(ImmutableMap.of("address1", "Address In Area"))
            .build();

    Result result = controller.hxCorrectAddress(request).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("Address Correction Results")
        .contains("Address In Area")
        .doesNotContain("validationOption")
        .doesNotContain("An error occurred");
  }
}
