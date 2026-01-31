package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.MessagesApi;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.ViewUtils;

public final class AdminLayoutFactory {

  private final ViewUtils viewUtils;
  private final SettingsManifest settingsManifest;
  private final TranslationLocales translationLocales;
  private final DeploymentType deploymentType;
  private final BundledAssetsFinder bundledAssetsFinder;
  private final MessagesApi messagesApi;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final ProfileUtils profileUtils;
  private final TemplateEngine templateEngine;

  @Inject
  public AdminLayoutFactory(
      ViewUtils viewUtils,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      BundledAssetsFinder bundledAssetsFinder,
      MessagesApi messagesApi,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      TemplateEngine templateEngine,
      ProfileUtils profileUtils) {
    this.viewUtils = viewUtils;
    this.settingsManifest = settingsManifest;
    this.translationLocales = translationLocales;
    this.deploymentType = deploymentType;
    this.bundledAssetsFinder = bundledAssetsFinder;
    this.messagesApi = messagesApi;
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);

    this.profileUtils = checkNotNull(profileUtils);
  }

  public AdminLayout getLayout(AdminLayout.NavPage navPage) {
    return new AdminLayout(
        viewUtils,
        navPage,
        settingsManifest,
        translationLocales,
        deploymentType,
        bundledAssetsFinder,
        messagesApi,
        playThymeleafContextFactory,
        templateEngine,
        profileUtils);
  }
}
