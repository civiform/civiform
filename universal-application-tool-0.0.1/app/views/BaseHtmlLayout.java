package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import javax.inject.Inject;
import play.twirl.api.Content;

public class BaseHtmlLayout {
  protected final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Returns HTTP content of type "text/html". */
  protected Content htmlContent(DomContent... domContents) {
    return new HtmlResponseContent(domContents);
  }

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
      return "text/html; charset=UTF-8";
    }
  }
}
