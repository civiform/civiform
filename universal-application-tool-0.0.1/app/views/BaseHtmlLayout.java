package views;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import play.twirl.api.Content;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;

public class BaseHtmlLayout {
  private final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  /** Returns HTTP content of type "text/html". */
  protected Content htmlContent(DomContent... domContents) {
    return new HtmlResponseContent(domContents);
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
