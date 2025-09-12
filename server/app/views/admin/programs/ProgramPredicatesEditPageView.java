package views.admin.programs;

import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

/**
 * Page view for rendering ProgramPredicatesEditPageView.html. This page is used for editing
 * predciates of a block in a program and replaces {@link ProgramPredicatesEditView} and {@link
 * ProgramPredicateConfigureView}.
 */
public class ProgramPredicatesEditPageView
    extends AdminLayoutBaseView<ProgramPredicatesEditPageViewModel> {
  @Inject
  public ProgramPredicatesEditPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils) {
    super(
        templateEngine, playThymeleafContextFactory, settingsManifest, assetsFinder, profileUtils);
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/ProgramPredicatesEditPageView.html";
  }
}
