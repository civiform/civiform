package views.admin.apibridge.programbridge;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.ViteService;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;

/** View object for rendering the program bridge edit page */
public class EditPageView extends AdminLayoutBaseView<EditPageViewModel> {
  @Inject
  public EditPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      ViteService viteService,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest, viteService, profileUtils);
  }

  @Override
  protected String pageTitle() {
    return "Edit Program Bridge Definitions";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/programbridge/EditPage";
  }
}
