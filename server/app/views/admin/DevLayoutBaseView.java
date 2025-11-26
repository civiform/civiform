package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.shared.AdminCommonHeader;

/**
 * {@link DevLayoutBaseView} is used to render the supplied Thymeleaf page template. This view is
 * tied to layout template {@link LayoutTemplate#ADMIN_LAYOUT}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class DevLayoutBaseView<TModel extends BaseViewModel> extends BaseView<TModel> {
  private final BundledAssetsFinder bundledAssetsFinder;

  public DevLayoutBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      BundledAssetsFinder bundledAssetsFinder) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
    this.bundledAssetsFinder = checkNotNull(bundledAssetsFinder);
  }

  /** Override to set the active page for top header navigation. */
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.NULL_PAGE;
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {

    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(activeNavigationPage())
            .isOnlyProgramAdmin(false)
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(request))
            .build());
  }

  @Override
  protected final String layoutTemplate() {
    return LayoutTemplate.ADMIN_LAYOUT;
  }

  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.path("dist/uswds.min.css"))
        .build();
  }

  @Override
  protected final ImmutableList<String> getSiteHeadScripts() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.path("dist/admin.bundle.js"))
        .add(bundledAssetsFinder.path("javascripts/uswds/uswds-init.min.js"))
        .build();
  }

  @Override
  protected final ImmutableList<String> getSiteBodyScripts() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.path("dist/uswds.bundle.js"))
        .build();
  }
}
