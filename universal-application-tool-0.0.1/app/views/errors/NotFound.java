package views.errors;

import static j2html.TagCreator.h1;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.inject.Inject;
import play.twirl.api.Content;
import views.style.BaseStyles;
import views.style.Styles;
import play.i18n.Messages;
import j2html.tags.ContainerTag;
import views.BaseHtmlView;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.style.ApplicantStyles;
import views.applicant.ApplicantLayout;

public class NotFound extends BaseHtmlView {

  //private final BaseHtmlLayout layout;
  private final ApplicantLayout layout;

  @Inject
  public NotFound(ApplicantLayout layout) {
    this.layout = layout;
  }

  public Content render() {
    String title = "Not Found";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent());

    return layout.render(htmlBundle);
  }

  private ContainerTag mainContent() {
    ContainerTag content = layout.branding();
    
    content.with(div(
        div(
          this.layout
            .viewUtils
            .makeLocalImageTag("ChiefSeattle_Blue")
            .withClasses(Styles.COL_START_1, Styles.BG_GRAY_300, Styles.OBJECT_CONTAIN, Styles.MAX_H_24, Styles.MAX_W_24),
          p("This page doesn't seem to exist").withClasses(Styles.FONT_BOLD),
          p("Please check the URL")
        )
    ).withClasses(Styles.BG_GRAY_300, Styles.GRID, Styles.GRID_COLS_2, Styles.CONTENT_CENTER, Styles.PL_4, Styles.PT_2));

    return content;
  }

  /*public Content render() {
    String pageTitle = "Page Not Found";
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(pageTitle).withClasses(Styles.MY_4),
                div().withClasses(Styles.INLINE_BLOCK));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(pageTitle).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
 }*/

}

            //div()
                //.withClasses(Styles.BG_GRAY_300, Styles.PL_4, Styles.FLEX_ROW)
                //.with(

                //)
