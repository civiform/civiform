package views.dev;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Test;

public class AddressCheckerPageViewModelTest {

  private static AddressCheckerPageViewModel.AddressCheckerPageViewModelBuilder baseBuilder() {
    return AddressCheckerPageViewModel.builder()
        .backLinkUrl("/dev/seed")
        .findAddressCandidatesUrls(Optional.empty())
        .addressServiceAreaValidationUrls(Optional.empty())
        .serviceAreaValidationConfigMap(ImmutableMap.of())
        .correctAddressActionUrl("/correct")
        .checkServiceAreaActionUrl("/check");
  }

  @Test
  public void showCorrectAddressForm_enabledWithUrls_isTrue() {
    var model =
        baseBuilder()
            .addressCorrectionEnabled(true)
            .findAddressCandidatesUrls(Optional.of(ImmutableList.of("http://esri")))
            .build();

    assertThat(model.showCorrectAddressForm()).isTrue();
  }

  @Test
  public void showCorrectAddressForm_disabled_isFalse() {
    var model =
        baseBuilder()
            .addressCorrectionEnabled(false)
            .findAddressCandidatesUrls(Optional.of(ImmutableList.of("http://esri")))
            .build();

    assertThat(model.showCorrectAddressForm()).isFalse();
  }

  @Test
  public void showCorrectAddressForm_noUrls_isFalse() {
    assertThat(baseBuilder().addressCorrectionEnabled(true).build().showCorrectAddressForm())
        .isFalse();

    var emptyUrls =
        baseBuilder()
            .addressCorrectionEnabled(true)
            .findAddressCandidatesUrls(Optional.of(ImmutableList.of()))
            .build();

    assertThat(emptyUrls.showCorrectAddressForm()).isFalse();
  }

  @Test
  public void showCheckServiceAreaForm_enabledWithUrls_isTrue() {
    var model =
        baseBuilder()
            .addressValidationEnabled(true)
            .addressServiceAreaValidationUrls(Optional.of(ImmutableList.of("http://esri")))
            .build();

    assertThat(model.showCheckServiceAreaForm()).isTrue();
  }

  @Test
  public void showCheckServiceAreaForm_disabled_isFalse() {
    var model =
        baseBuilder()
            .addressValidationEnabled(false)
            .addressServiceAreaValidationUrls(Optional.of(ImmutableList.of("http://esri")))
            .build();

    assertThat(model.showCheckServiceAreaForm()).isFalse();
  }

  @Test
  public void showCheckServiceAreaForm_noUrls_isFalse() {
    assertThat(baseBuilder().addressValidationEnabled(true).build().showCheckServiceAreaForm())
        .isFalse();
  }
}
