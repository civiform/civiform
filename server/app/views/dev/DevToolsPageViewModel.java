package views.dev;

import com.google.common.collect.ImmutableList;
import controllers.dev.routes;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

/** View model for the dev tools page. */
@Data
@Builder
public class DevToolsPageViewModel implements BaseViewModel {
  private final boolean isDev;
  private final ImmutableList<String> durableJobOptions;
  private final String csrfToken;

  public String getSeedProgramsUrl() {
    return routes.DevToolsController.seedPrograms().url();
  }

  public String getSeedQuestionsUrl() {
    return routes.DevToolsController.seedQuestions().url();
  }

  public String getClearUrl() {
    return routes.DevToolsController.clear().url();
  }

  public String getClearCacheUrl() {
    return routes.DevToolsController.clearCache().url();
  }

  public String getRunDurableJobUrl() {
    return routes.DevToolsController.runDurableJob().url();
  }

  public String getIconsUrl() {
    return controllers.dev.routes.IconsController.index().url();
  }

  public String getHomeUrl() {
    return controllers.routes.HomeController.index().url();
  }

  public String getAddressToolsUrl() {
    return controllers.dev.routes.AddressCheckerController.index().url();
  }

  public String getSessionProfileUrl() {
    return controllers.dev.routes.ProfileController.index().url();
  }

  public String getSessionDisplayUrl() {
    return controllers.dev.routes.SessionDisplayController.index().url();
  }
}
