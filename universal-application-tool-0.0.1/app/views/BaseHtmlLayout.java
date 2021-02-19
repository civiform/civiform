package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It
 * should have a `render` method that takes the DOM contents for the main tag.
 */
public class BaseHtmlLayout extends BaseHtmlView {
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
   * Returns a link tag for common CSS that should be included in the head of most other layouts.
   */
  protected Tag getCommonCssTag() {
    return viewUtils.makeLocalCssTag("common");
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
