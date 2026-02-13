package views.trustedintermediary;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class TiEditClientPageViewModel implements BaseViewModel {
  private final String tiGroupName;
  private final boolean isEdit;
  private final String formActionUrl;
  private final boolean isNameSuffixEnabled;

  private final String firstName;
  private final String middleName;
  private final String lastName;
  private final String nameSuffix;
  private final String phoneNumber;
  private final String emailAddress;
  private final String dateOfBirth;
  private final String tiNote;

  private final List<SuffixOption> suffixOptions;

  public String getBackToClientListUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(1))
        .url();
  }

  public String getCancelUrl() {
    return getBackToClientListUrl();
  }

  public String getAccountSettingsUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.accountSettings().url();
  }

  public String getClientListUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(1))
        .url();
  }

  @Data
  @Builder
  public static final class SuffixOption {
    private final String label;
    private final String value;
  }
}
