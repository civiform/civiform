package views.admin;

import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  private final String BODY_STYLES =
      StyleUtils.joinStyles(
          BaseStyles.BODY_GRADIENT_STYLE,
          Styles.BOX_BORDER,
          Styles.H_SCREEN,
          Styles.W_SCREEN,
          Styles.OVERFLOW_HIDDEN,
          Styles.FLEX);
  private final String CENTERED_STYLES =
      StyleUtils.joinStyles(Styles.PX_2, Styles.MAX_W_SCREEN_XL, Styles.MX_AUTO);
  private final String FULL_STYLES = StyleUtils.joinStyles(Styles.FLEX, Styles.FLEX_ROW);
  private final String MAIN_STYLES =
      StyleUtils.joinStyles(
          Styles.BG_WHITE,
          Styles.BORDER,
          Styles.BORDER_GRAY_200,
          Styles.MT_12,
          Styles.OVERFLOW_Y_AUTO,
          Styles.SHADOW_LG,
          Styles.W_SCREEN);

  protected Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(MAIN_STYLES, isCentered ? CENTERED_STYLES : FULL_STYLES);
    bundle.addBodyStyles(BODY_STYLES);
    return htmlContent(bundle);
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, true);
  }

  public Content renderFull(HtmlBundle bundle) {
    return render(bundle, false);
  }
}
