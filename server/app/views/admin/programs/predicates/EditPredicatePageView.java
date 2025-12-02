package views.admin.programs.predicates;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;

/**
 * Page view for rendering EditPredicatePageView.html. This page is used for editing predicates of a
 * block in a program and replaces {@link ProgramPredicatesEditView} and {@link
 * ProgramPredicateConfigureView}.
 */
public class EditPredicatePageView extends AdminLayoutBaseView<EditPredicatePageViewModel> {
  @Inject
  public EditPredicatePageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        settingsManifest,
        bundledAssetsFinder,
        profileUtils);
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/EditPredicatePageView.html";
  }

  @Override
  protected boolean isWidescreen() {
    return true;
  }
}
