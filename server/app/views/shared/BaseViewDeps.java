package views.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.Environment;
import services.settings.SettingsManifest;

/** Common BaseView Dependencies */
public record BaseViewDeps(
    TemplateEngine templateEngine,
    ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
    SettingsManifest settingsManifest,
    Environment environment) {
  @Inject
  public BaseViewDeps(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      Environment environment) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.environment = checkNotNull(environment);
  }
}
