package views.admin;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.main;

import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.ViewUtils;

public class AdminProgramLayout extends BaseHtmlLayout {

  @Inject
  public AdminProgramLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(DomContent... mainDomContents) {
    return htmlContent(head(getCommonCssTag()), body(h1("Program"), main(mainDomContents)));
  }
}
