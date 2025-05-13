package views.admin.apibridge.discovery;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import views.admin.apibridge.BaseView;

public class DiscoveryDetailsView extends BaseView<DiscoveryDetailsViewModel> {

  @Inject
  public DiscoveryDetailsView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder);
  }

  @Override
  protected String thymeleafTemplate() {
    return "admin/apibridge/discovery/DiscoveryDetails";
  }

  @Override
  protected void customizeContext(ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable(
        "routesDiscoveryController", controllers.admin.apibridge.routes.DiscoveryController);
  }
}
