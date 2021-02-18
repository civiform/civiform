package views;

import j2html.tags.DomContent;
import play.twirl.api.Content;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class BaseHtmlLayout {
  private final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  protected Content htmlContent(DomContent... domContents) {
    return new BaseHtmlView.HtmlResponseContent(domContents);
  }
}
