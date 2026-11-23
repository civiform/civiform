package views.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Optional;
import modules.ThymeleafModule;
import play.mvc.Http;
import services.BundledAssetsFinder;
import services.DeploymentType;
import views.BaseView;
import views.BaseViewModel;
import views.LayoutTemplate;

/**
 * {@link UnauthenticatedLayoutBaseView} renders the supplied Thymeleaf page template inside a bare
 * shell with no navigation, sidebar, or footer. Use it for pages that must render without a signed
 * in user. This view is tied to layout template {@link LayoutTemplate#UNAUTHENTICATED_LAYOUT}.
 *
 * <p>A profile is never required. If one is present it is only used to highlight the active role in
 * the demo banner.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class UnauthenticatedLayoutBaseView<TModel extends BaseViewModel>
    extends BaseView<TModel> {
  private final BundledAssetsFinder bundledAssetsFinder;
  private final ProfileUtils profileUtils;
  private final DeploymentType deploymentType;
  private final Clock clock;

  public UnauthenticatedLayoutBaseView(LayoutDeps layoutDeps) {
    checkNotNull(layoutDeps);
    super(layoutDeps.baseViewDeps());
    this.bundledAssetsFinder = layoutDeps.bundledAssetsFinder();
    this.profileUtils = layoutDeps.profileUtils();
    this.deploymentType = layoutDeps.deploymentType();
    this.clock = layoutDeps.clock();
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    boolean isDevOrStaging = deploymentType.isDevOrStaging();

    context.setVariable(
        "demoBanner",
        DemoBanner.create(
            settingsManifest,
            request,
            profileUtils.optionalCurrentUserProfile(request),
            isDevOrStaging,
            clock));

    // Shows the toast warning against entering real data
    context.setVariable("isDevOrStaging", isDevOrStaging);
  }

  @Override
  protected final Optional<LayoutTemplate> layoutTemplate() {
    return Optional.of(LayoutTemplate.UNAUTHENTICATED_LAYOUT);
  }

  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.of(bundledAssetsFinder.getNorthStarStylesheet());
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

  /**
   * The applicant bundle, then USWDS. The browser tests' page fixture waits for the applicant
   * bundle's main.ts and modal.ts to initialize.
   */
  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(
            ScriptElementSettings.builder().src(bundledAssetsFinder.getApplicantJsBundle()).build())
        .add(ScriptElementSettings.builder().src(bundledAssetsFinder.getUswdsJsBundle()).build())
        .build();
  }
}
