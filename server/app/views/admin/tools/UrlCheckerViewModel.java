package views.admin.tools;

import controllers.admin.tools.routes;
import views.BaseViewModel;

public record UrlCheckerViewModel() implements BaseViewModel {
  public String testUrlEndpoint() {
    return routes.AdminToolsController.hxTestUrl().url();
  }
}
