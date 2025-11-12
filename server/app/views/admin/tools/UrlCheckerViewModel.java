package views.admin.tools;

import controllers.admin.tools.routes;
import views.admin.BaseViewModel;

public record UrlCheckerViewModel() implements BaseViewModel {
  public String testUrlEndpoint() {
    return routes.AdminToolsController.hxTestUrl().url();
  }
}
