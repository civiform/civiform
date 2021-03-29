package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.document;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.Arrays;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.components.Icons;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It
 * should have a `render` method that takes the DOM contents for the main tag.
 */
public class BaseHtmlLayout extends BaseHtmlView {
  private static final String BANNER_TEXT =
      "DO NOT enter actual or personal data in this demo site";
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";

  protected final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Returns HTTP content of type "text/html". */
  public Content htmlContent(DomContent... domContents) {
    return new HtmlResponseContent(addDemoBanner(domContents));
  }

  /**
   * Returns a script tag that loads Tailwindcss styles and configurations common to all pages in
   * the CiviForm.
   *
   * <p>This should be added to the end of the body of all layouts. Adding it to the end of the body
   * allows the page to begin rendering before the script is loaded.
   *
   * <p>Adding this to a page allows Tailwindcss utility classes to be be usable on that page.
   */
  public Tag tailwindStyles() {
    return viewUtils.makeLocalCssTag(TAILWIND_COMPILED_FILENAME);
  }

  protected static class HtmlResponseContent implements Content {
    private final DomContent[] domContents;

    protected HtmlResponseContent(DomContent... domContents) {
      this.domContents = checkNotNull(domContents);
    }

    @Override
    public String body() {
      return document(new ContainerTag("html").with(domContents));
    }

    @Override
    public String contentType() {
      return "text/html";
    }
  }

  public ContainerTag demoBanner() {
    return div()
        .withClasses(demoBannerStyles())
        .with(
            div()
                .withClasses(
                    Styles.MAX_W_7XL,
                    Styles.MX_AUTO,
                    Styles.PY_3,
                    Styles.PX_3,
                    StyleUtils.responsiveSmall(Styles.PX_6),
                    StyleUtils.responsiveLarge(Styles.PX_8))
                .with(
                    div()
                        .withClasses(
                            Styles.FLEX,
                            Styles.ITEMS_CENTER,
                            Styles.JUSTIFY_BETWEEN,
                            Styles.FLEX_WRAP)
                        .with(
                            div()
                                .withClasses(
                                    Styles.W_0, Styles.FLEX_1, Styles.FLEX, Styles.ITEMS_CENTER)
                                .with(
                                    span()
                                        .withClasses(
                                            addBgOpacity(
                                                Styles.FLEX,
                                                Styles.P_2,
                                                Styles.ROUNDED_LG,
                                                Styles.BG_RED_800))
                                        .with(
                                            Icons.svg(Icons.LOGIN_BANNER_PATH, 24)
                                                .withClasses(
                                                    Styles.H_6, Styles.W_6, Styles.TEXT_WHITE)
                                                .attr("fill", "none")
                                                .attr("stroke-linecap", "round")
                                                .attr("stroke-linejoin", "round")
                                                .attr("stroke-width", "2")),
                                    p().withClasses(
                                            Styles.ML_3,
                                            Styles.FONT_MEDIUM,
                                            Styles.TEXT_WHITE,
                                            Styles.TRUNCATE)
                                        .with(span(BANNER_TEXT))))));
  }

  private String[] addBgOpacity(String... styles) {
    String[] newStyles = Arrays.copyOf(styles, styles.length + 1);
    newStyles[styles.length] = Styles.BG_OPACITY_80;
    return newStyles;
  }

  private boolean isAdminPage() {
    return this.getClass().getCanonicalName().startsWith("views.admin");
  }

  private String[] demoBannerStyles() {
    if (isAdminPage()) {
      return addBgOpacity(Styles.BG_RED_600, Styles.ABSOLUTE, Styles.BOTTOM_2, Styles.W_FULL);
    }
    return new String[] {Styles.BG_RED_600};
  }

  private DomContent[] addDemoBanner(DomContent... domContents) {
    if (isAdminPage()) {
      return appendDomContent(domContents, demoBanner());
    }
    return prependDomContent(domContents, demoBanner());
  }

  private static DomContent[] appendDomContent(DomContent[] domContents, DomContent element) {
    DomContent[] newContents = Arrays.copyOf(domContents, domContents.length + 1);
    newContents[domContents.length] = element;
    return newContents;
  }

  private static DomContent[] prependDomContent(DomContent[] domContents, DomContent element) {
    DomContent[] newContents = Arrays.copyOf(domContents, domContents.length + 1);
    System.arraycopy(domContents, 0, newContents, 1, domContents.length);
    newContents[0] = element;
    return newContents;
  }
}
