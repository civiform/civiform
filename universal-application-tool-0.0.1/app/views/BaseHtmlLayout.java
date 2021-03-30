package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.document;
import static j2html.TagCreator.span;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
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
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";

  private final String BANNER_TEXT = "Do not enter actual or personal data in this demo site";

  protected final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Returns HTTP content of type "text/html". */
  public Content htmlContent(DomContent... domContents) {
    return new HtmlResponseContent(domContents);
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

  public Tag warningMessage() {
    ContainerTag wrappedWarningSvg =
        div()
            .withClasses(Styles.FLEX_NONE, Styles.PR_2)
            .with(
                Icons.svg(Icons.WARNING_SVG_PATH, 20)
                    .attr("fill-rule", "evenodd")
                    .withClasses(Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6));
    ContainerTag messageSpan = span(BANNER_TEXT);
    ContainerTag dismissButton =
        div("x")
            .withId("warning-message-dismiss")
            .withClasses(
                Styles.FONT_BOLD,
                Styles.PL_6,
                Styles.OPACITY_40,
                Styles.CURSOR_POINTER,
                StyleUtils.hover(Styles.OPACITY_100));

    return div(wrappedWarningSvg, messageSpan, dismissButton)
        .withId("warning-message")
        .withClasses(
            Styles.ABSOLUTE,
            Styles.FLEX,
            Styles.FLEX_ROW,
            Styles.BG_RED_400,
            Styles.BORDER_RED_500,
            Styles.BG_OPACITY_90,
            Styles.MAX_W_MD,
            Styles.PX_2,
            Styles.PY_2,
            Styles.TEXT_GRAY_700,
            Styles.TOP_2,
            Styles.ROUNDED_SM,
            Styles.SHADOW_LG,
            Styles.TRANSFORM,
            Styles._TRANSLATE_X_1_2,
            Styles.LEFT_1_2,
            Styles.HIDDEN);
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
}
