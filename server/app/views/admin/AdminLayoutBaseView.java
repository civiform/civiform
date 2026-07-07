package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

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
import views.admin.shared.Footer;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

/**
 * {@link AdminLayoutBaseView} is used to render the supplied Thymeleaf page template. This view is
 * tied to layout template {@link LayoutTemplate#ADMIN_LAYOUT}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class AdminLayoutBaseView<TModel extends BaseViewModel> extends BaseView<TModel> {
  protected final BundledAssetsFinder bundledAssetsFinder;
  protected final ProfileUtils profileUtils;

  public AdminLayoutBaseView(LayoutDeps layoutDeps) {
    checkNotNull(layoutDeps);
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

    // Set values for the footer
    context.setVariable(
        "footer",
        Footer.builder()
            .technicalSupportEmail(
                settingsManifest
                    .getSupportEmailAddress(request)
                    .orElse("SUPPORT EMAIL ADDRESS MISSING"))
            .build());
  }

  @Override
  protected final Optional<LayoutTemplate> layoutTemplate() {
    return Optional.of(LayoutTemplate.ADMIN_LAYOUT);
  }

  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder()
        .add(bundledAssetsFinder.getUswdsStylesheet())
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
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getAdminJsBundle()).build())
        .build();
  }

  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getUswdsJsBundle()).build())
        .build();
  }
}
