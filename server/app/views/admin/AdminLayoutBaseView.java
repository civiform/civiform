package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

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
 * {@link AdminLayoutBaseView} is used to render the supplied Thymeleaf page template. This view is
 * tied to layout template {@link LayoutTemplate#ADMIN_LAYOUT}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class AdminLayoutBaseView<TModel extends BaseViewModel> extends BaseView<TModel> {
  protected final ProfileUtils profileUtils;
  protected final AssetsFinder assetsFinder;

  public AdminLayoutBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
    this.profileUtils = checkNotNull(profileUtils);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /** Override to set the active page for top header navigation. */
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.NULL_PAGE;
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(activeNavigationPage())
            .isOnlyProgramAdmin(profile.isOnlyProgramAdmin())
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(request))
            .build());
  }

  @Override
  protected final String layoutTemplate() {
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
