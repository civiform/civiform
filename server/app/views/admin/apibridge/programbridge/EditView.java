package views.admin.apibridge.programbridge;

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
    return "programbridge-edit";
  }

  @Override
  protected String thymeleafTemplate() {
    return "admin/apibridge/programbridge/Edit";
  }

  @Override
  protected void customizeContext(ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable("currentPage", "list");
    context.setVariable(
        "routesProgramBridgeController",
        controllers.admin.apibridge.routes.ProgramBridgeController);
  }
}
