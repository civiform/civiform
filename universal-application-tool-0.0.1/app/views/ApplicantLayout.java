package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.*;

import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;

public class ApplicantLayout extends BaseHtmlLayout {
  private final ViewUtils viewUtils;

  @Inject
  public ApplicantLayout(ViewUtils viewUtils) {
    super(viewUtils);
    this.viewUtils = checkNotNull(viewUtils);
  }

  protected Content render(DomContent... domContents) {
    return htmlContent(head(viewUtils.makeLocalCssTag("common")), body(main(domContents)));
  }
}
