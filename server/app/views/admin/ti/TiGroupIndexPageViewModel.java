package views.admin.ti;

import controllers.admin.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class TiGroupIndexPageViewModel implements BaseViewModel {
  private final String providedName;
  private final String providedDescription;
  private final Optional<String> errorMessage;
  private final List<TiGroupRow> groups;

  public String getCreateActionUrl() {
    return routes.TrustedIntermediaryManagementController.create().url();
  }

  @Data
  @Builder
  public static final class TiGroupRow {
    private final long id;
    private final String name;
    private final String description;
    private final int memberCount;
    private final int clientCount;

    public String getEditUrl() {
      return routes.TrustedIntermediaryManagementController.edit(id).url();
    }

    public String getDeleteActionUrl() {
      return routes.TrustedIntermediaryManagementController.delete(id).url();
    }
  }
}
