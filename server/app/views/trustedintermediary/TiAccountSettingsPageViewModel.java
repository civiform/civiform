package views.trustedintermediary;

import controllers.ti.routes;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class TiAccountSettingsPageViewModel implements BaseViewModel {
  private final String tiGroupName;
  private final List<OrgMemberRow> orgMembers;

  @Data
  @Builder
  public static final class OrgMemberRow {
    private final String name;
    private final String email;
    private final String accountStatus;
  }

  public String getAccountSettingsUrl() {
    return routes.TrustedIntermediaryController.accountSettings().url();
  }

  public String getClientListUrl() {
    return controllers.ti.routes.TrustedIntermediaryController.dashboard(
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.empty(),
            java.util.Optional.of(1))
        .url();
  }
}
