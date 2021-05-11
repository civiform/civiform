package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.components.ToastMessage;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It
 * should have a `render` method that takes the DOM contents for the main tag.
 */
public class BaseHtmlLayout extends BaseHtmlView {
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";
  private static final String TRACKING_TAG_ID = "G-HXM0Y35TGE";
  /** also exists in ga.ts javascript assets */
  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";

  public final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Returns HTTP content of type "text/html". */
  public Content htmlContent(DomContent... domContents) {
    ArrayList<DomContent> contents = new ArrayList<>(Arrays.asList(domContents));
    ToastMessage privacyBanner =
        ToastMessage.error(BANNER_TEXT).setId("warning-message").setIgnorable(true).setDuration(0);
    contents.add(0, privacyBanner.getContainerTag());
    contents.add(viewUtils.makeLocalJsTag("toast"));
    contents.add(viewUtils.makeLocalJsTag("radio"));
    return new HtmlResponseContent(contents.toArray(new DomContent[0]));
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

  public Tag headContent(String... titlestr) {
    return viewUtils.makeHead(TAILWIND_COMPILED_FILENAME, TRACKING_TAG_ID, titlestr);
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
