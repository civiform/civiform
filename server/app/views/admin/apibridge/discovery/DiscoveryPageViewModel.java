package views.admin.apibridge.discovery;

import controllers.admin.apibridge.routes;
import views.admin.BaseViewModel;

/** Contains all the properties for rendering the DiscoveryPage.html */
public record DiscoveryPageViewModel() implements BaseViewModel {
  public String currentPage() {
    return "discovery";
  }

  public String hxDiscoveryPopulateUrl() {
    return routes.DiscoveryController.hxDiscoveryPopulate().url();
  }
}
