package views.admin.apibridge.bridgeconfiguration;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import views.admin.apibridge.BaseView;

public class EditView extends BaseView<EditViewModel> {
  @Inject
  public EditView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder);
  }

  @Override
  protected String pageTitle() {
    return "apibridge-edit";
  }

  @Override
  protected String thymeleafTemplate() {
    return "admin/apibridge/bridgeconfiguration/Edit";
  }

  @Override
  protected void customizeContext(ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable("currentPage", "list");
    context.setVariable(
        "routesBridgeConfigurationController",
        controllers.admin.apibridge.routes.BridgeConfigurationController);
  }
}
