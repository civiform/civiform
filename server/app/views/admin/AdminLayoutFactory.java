package views.admin;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final Config configuration;

  @Inject
  public AdminLayoutFactory(ViewUtils viewUtils, Config configuration) {
    this.viewUtils = viewUtils;
    this.configuration = configuration;
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(viewUtils, configuration, navPage);
  }
}
