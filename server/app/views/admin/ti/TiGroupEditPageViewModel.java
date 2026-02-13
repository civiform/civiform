package views.admin.ti;

import controllers.admin.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class TiGroupEditPageViewModel implements BaseViewModel {
  private final String groupName;
  private final String groupDescription;
  private final long tiGroupId;
  private final String providedEmailAddress;
  private final Optional<String> errorMessage;
  private final List<TiMemberRow> members;

  public String getBackUrl() {
    return routes.TrustedIntermediaryManagementController.index().url();
  }

  public String getAddMemberActionUrl() {
    return routes.TrustedIntermediaryManagementController.addIntermediary(tiGroupId).url();
  }

  @Data
  @Builder
  public static final class TiMemberRow {
    private final String displayName;
    private final String emailAddress;
    private final String status;
    private final long accountId;
    private final long tiGroupId;

    public String getRemoveActionUrl() {
      return routes.TrustedIntermediaryManagementController.removeIntermediary(tiGroupId).url();
    }
  }
}
