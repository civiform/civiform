package views.support;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.i18n.Messages;
import services.BundledAssetsFinder;
import views.BaseView;
import views.BaseViewModel;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the unsupported browser page, rendering the standalone document
 * UnsupportedBrowserPage.html without a layout. The page is static, so the model carries nothing.
 */
public final class UnsupportedBrowserPageView extends BaseView<UnsupportedBrowserPageView.Model> {

  /** The page has no dynamic data. */
  public record Model() implements BaseViewModel {}

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
}
