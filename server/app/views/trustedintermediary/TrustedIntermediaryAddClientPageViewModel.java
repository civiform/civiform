package views.trustedintermediary;

import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the trusted intermediary add client page (Thymeleaf). */
@Data
@Builder
public final class TrustedIntermediaryAddClientPageViewModel implements BaseViewModel {

  private final String tiGroupName;

  // Targets of the tab links under the group name. The client list URL is also the target of the
  // back link, the form's Cancel button and the "Back to client list" button.
  private final String clientListUrl;
  private final String accountSettingsUrl;

  // The client was just saved; shows the success alert
  private final boolean showSuccessAlert;

  // Name of the client that was just saved, shown in the success alert; null until saved
  private final String newClientName;

  // Target of the "Start an application" button for the client that was just saved. When set, the
  // buttons replace the form. Null until saved.
  private final String startApplicationUrl;

  private final TrustedIntermediaryClientInfoForm form;
}
