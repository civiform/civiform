package views.admin;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.settings.SettingsManifest;
import views.admin.shared.AdminCommonHeader;

/**
 * Admin base view class used to render the supplied Thymeleaf page template. This view is tied to
 * layout template file {@code AdminLayout.html}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class AdminBaseView<TModel extends BaseViewModel> extends BaseView<TModel> {
  public AdminBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils) {
    super(
        templateEngine, playThymeleafContextFactory, settingsManifest, assetsFinder, profileUtils);
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    context.setVariable(
        "adminCommonHeader",
        new AdminCommonHeader(activeNavigationPage(), profile.isOnlyProgramAdmin()));
  }

  @Override
  protected String layoutTemplate() {
    return LayoutTemplate.ADMIN_LAYOUT;
  }

  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder().add(assetsFinder.path("dist/uswds.min.css")).build();
  }

  @Override
  protected final ImmutableList<String> getSiteHeadScripts() {
    return ImmutableList.<String>builder()
        .add(assetsFinder.path("dist/admin.bundle.js"))
        .add(assetsFinder.path("javascripts/uswds/uswds-init.min.js"))
        .build();
  }

  @Override
  protected final ImmutableList<String> getSiteBodyScripts() {
    return ImmutableList.<String>builder().add(assetsFinder.path("dist/uswds.bundle.js")).build();
  }
}
