package views;

import j2html.tags.DomContent;
import play.twirl.api.Content;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.head;

public class ApplicantLayout extends BaseHtmlLayout {
  private final ViewUtils viewUtils;

  @Inject
  public ApplicantLayout(ViewUtils viewUtils) {
    super(viewUtils);
    this.viewUtils = checkNotNull(viewUtils);
  }

  protected Content render(DomContent... domContents) {
    return htmlContent(head(viewUtils.makeLocalCssTag("common")), body(domContents));
  }
}

// TODO(natsid):
// Create BaseHtmlLayout class that has htmlContent method.
// Layout classes will extend BaseHtmlLayout.
// Other view classes will inject their necessary layout classes and extend BaseHtmlView.
