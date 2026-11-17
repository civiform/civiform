package views.trustedintermediary;

import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the trusted intermediary edit client page (Thymeleaf). */
@Data
@Builder
public final class TrustedIntermediaryEditClientPageViewModel implements BaseViewModel {

  private final String tiGroupName;

  // Targets of the tab links under the group name. The client list URL is also the target of the
  // back link and the form's Cancel button.
  private final String clientListUrl;
  private final String accountSettingsUrl;

  // The client's information was just saved; shows the success alert above the form
  private final boolean showSuccessAlert;

  private final TrustedIntermediaryClientInfoForm form;
}
