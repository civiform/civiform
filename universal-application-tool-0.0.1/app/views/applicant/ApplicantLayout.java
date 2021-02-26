package views.applicant;

import static j2html.TagCreator.body;
import static j2html.TagCreator.head;
import static j2html.TagCreator.main;
import static j2html.TagCreator.title;

import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.ViewUtils;

public class ApplicantLayout extends BaseHtmlLayout {

  @Inject
  public ApplicantLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(DomContent... mainDomContents) {
    return htmlContent(
        head().with(title("Applicant layout title")),
        body().with(main(mainDomContents), tailwindStyles()));
  }
}
