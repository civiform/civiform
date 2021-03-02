package views.admin;

import static j2html.TagCreator.body;
import static j2html.TagCreator.head;
import static j2html.TagCreator.main;

import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.Styles;
import views.ViewUtils;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the admin layout. */
  public Content render(DomContent... mainDomContents) {
    return htmlContent(
        head(tailwindStyles()),
        body(main(mainDomContents).withClasses(Styles.MAX_W_SCREEN_XL, Styles.MX_AUTO)));
  }
}
