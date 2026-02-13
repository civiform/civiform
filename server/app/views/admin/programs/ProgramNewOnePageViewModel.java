package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramNewOnePageViewModel implements BaseViewModel {
  private final boolean externalProgramCardsEnabled;
  private final ImmutableList<CategoryOption> categoryOptions;
  private final ImmutableList<TiGroupOption> tiGroupOptions;
  private final String defaultNotificationPreferenceValue;
  private final Optional<String> errorMessage;

  public String getFormActionUrl() {
    return routes.AdminProgramController.create().url();
  }

  @Data
  @Builder
  public static class CategoryOption {
    private final long id;
    private final String name;
  }

  @Data
  @Builder
  public static class TiGroupOption {
    private final long id;
    private final String name;
  }
}
