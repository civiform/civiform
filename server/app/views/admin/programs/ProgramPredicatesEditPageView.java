package views.admin.programs;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

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

  @Override
  protected ImmutableList<String> getPageStylesheets() {
    return ImmutableList.of(assetsFinder.path("stylesheets/tailwind.css"));
  }

  @Override
  protected ImmutableList<String> getPageBodyScripts() {
    return ImmutableList.of();
  }
}
