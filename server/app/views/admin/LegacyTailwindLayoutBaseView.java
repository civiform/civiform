package views.admin;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import modules.ThymeleafModule;
import play.mvc.Http;
import services.BundledAssetsFinder;
import views.BaseView;
import views.BaseViewModel;
import views.LayoutTemplate;
import views.admin.shared.AdminCommonHeader;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

/**
 * {@link LegacyTailwindLayoutBaseView} is used to render the supplied Thymeleaf page template. This
 * view is tied to layout template {@link LayoutTemplate#LEGACY_TAILWIND_LAYOUT}, which mirrors the
 * legacy j2html admin page skeleton, including the Tailwind stylesheet, so that 1:1 page migrations
 * keep their original styling.
 *
 * <p>This view will be phased out during the admin redesign. Use it only for pages migrated 1:1
 * from j2html; new designs should use {@link AdminLayoutBaseView}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class LegacyTailwindLayoutBaseView<TModel extends BaseViewModel>
    extends BaseView<TModel> {
  private final BundledAssetsFinder bundledAssetsFinder;
  protected final ProfileUtils profileUtils;

  public LegacyTailwindLayoutBaseView(LayoutDeps layoutDeps) {
    super(layoutDeps.baseViewDeps());
    this.bundledAssetsFinder = layoutDeps.bundledAssetsFinder();
    this.profileUtils = layoutDeps.profileUtils();
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
  protected final Optional<LayoutTemplate> layoutTemplate() {
    return Optional.of(LayoutTemplate.LEGACY_TAILWIND_LAYOUT);
  }

  /** Stylesheets in the same order the legacy j2html layout added them. */
  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.getUswdsStylesheet())
        .add(bundledAssetsFinder.getTailwindStylesheet())
        .add(bundledAssetsFinder.getMapLibreGLStylesheet())
        .build();
  }

  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteHeadScripts() {
    var builder = ImmutableList.<ScriptElementSettings>builder();

    if (bundledAssetsFinder.useBundlerDevServer()) {
      builder.add(ScriptElementSettings.builder().src(bundledAssetsFinder.viteClientUrl()).build());
    }

    return builder
        .add(
            ScriptElementSettings.builder()
                .src(bundledAssetsFinder.getUswdsJsInit())
                .type("text/javascript")
                .build())
        .build();
  }

  /** The legacy j2html layout loaded both JS bundles from a footer at the end of the body. */
  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getAdminJsBundle()).build())
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getUswdsJsBundle()).build())
        .build();
  }
}
