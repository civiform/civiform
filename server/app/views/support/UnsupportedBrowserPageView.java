package views.support;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.i18n.Messages;
import services.BundledAssetsFinder;
import views.BaseView;
import views.BaseViewModel;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

/**
 * Thymeleaf view for the unsupported browser page, rendering the standalone document
 * UnsupportedBrowserPage.html without a layout.
 */
public final class UnsupportedBrowserPageView extends BaseView<UnsupportedBrowserPageView.Model> {

  /**
   * Model for the unsupported browser page.
   *
   * @param isDevOrStaging Add the toast warning against entering real data
   */
  public record Model(boolean isDevOrStaging) implements BaseViewModel {}

  private final BundledAssetsFinder bundledAssetsFinder;

  @Inject
  public UnsupportedBrowserPageView(LayoutDeps layoutDeps) {
    super(layoutDeps.baseViewDeps());
    this.bundledAssetsFinder = layoutDeps.bundledAssetsFinder();
  }

  @Override
  protected String pageTitle(Model model, Messages messages) {
    return "The browser you are currently using is not supported — CiviForm";
  }

  @Override
  protected String pageTemplate() {
    return "support/UnsupportedBrowserPage.html";
  }

  /** The page only uses Tailwind classes. */
  @Override
  protected ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.of(bundledAssetsFinder.getTailwindStylesheet());
  }

  /**
   * The applicant bundle, then USWDS, as the legacy layout loaded them. The browser tests' page
   * fixture waits for the applicant bundle to initialize.
   */
  @Override
  protected ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.of(
        ScriptElementSettings.builder().src(bundledAssetsFinder.getApplicantJsBundle()).build(),
        ScriptElementSettings.builder().src(bundledAssetsFinder.getUswdsJsBundle()).build());
  }
}
