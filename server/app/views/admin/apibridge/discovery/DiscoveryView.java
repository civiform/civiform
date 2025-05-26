package views.admin.apibridge.discovery;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import views.admin.apibridge.BaseView;

public class DiscoveryView extends BaseView<DiscoveryViewModel> {

  @Inject
  public DiscoveryView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder);
  }

  @Override
  protected String pageTitle() {
    return "apibridge-discovery";
  }

  @Override
  protected String thymeleafTemplate() {
    return "admin/apibridge/discovery/Discovery";
  }

  @Override
  protected void customizeContext(ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable("currentPage", "discovery");
    context.setVariable(
        "routesDiscoveryController", controllers.admin.apibridge.routes.DiscoveryController);
  }
}
