package views.errors;

import static j2html.TagCreator.h1;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.inject.Inject;
import play.twirl.api.Content;
import views.style.BaseStyles;
import views.style.Styles;
import views.style.StyleUtils;
import play.i18n.Messages;
import j2html.tags.ContainerTag;
import play.mvc.Http;
import views.BaseHtmlView;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.style.ErrorStyles;
import views.applicant.ApplicantLayout;
import play.i18n.MessagesApi;
import services.MessageKey;

public class NotFound extends BaseHtmlView {

  //private final BaseHtmlLayout layout;
  private final ApplicantLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public NotFound(ApplicantLayout layout, MessagesApi messagesApi) {
    this.layout = layout;
    this.messagesApi = messagesApi;
  }

  /*public Content render() {
    String title = "Not Found";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent());

    return layout.render(htmlBundle);
  }*/

  /*private ContainerTag mainContent() {
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
  }*/

  public Content render(
      Http.Request request,
      Messages messages) {
    HtmlBundle bundle = layout.getBundle();
    bundle.addMainContent(
        topContent(
            messages.at(MessageKey.ERROR_NOT_FOUND.getKeyName()),
            messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION.getKeyName())));

    return layout.render(bundle);
  }

  private ContainerTag topContent(String titleText, String infoTextLine) {
    // "Get benefits"
    ContainerTag branding = layout.branding();

    ContainerTag programIndexH1 =
        h1().withText(titleText)
            .withClasses(
                Styles.TEXT_4XL,
                StyleUtils.responsiveSmall(Styles.TEXT_5XL),
                Styles.FONT_SEMIBOLD,
                Styles.MB_2,
                StyleUtils.responsiveSmall(Styles.MB_6));

    ContainerTag infoLine1Div =
        div()
            .withText(infoTextLine)
            .withClasses(Styles.TEXT_SM, StyleUtils.responsiveSmall(Styles.TEXT_BASE));

    ContainerTag seattleLogoDiv =
        div()
            .with(
                this.layout
                    .viewUtils
                    .makeLocalImageTag("Seattle-logo_horizontal_blue-white_small")
                    .attr("width", 175)
                    .attr("height", 70))
            .withClasses(Styles.ABSOLUTE, Styles.TOP_2, Styles.LEFT_2);

    return div(branding, div()
        .withId("top-content")
        .withClasses(ErrorStyles.NOT_FOUND_TOP_CONTENT, Styles.RELATIVE)
        .with(seattleLogoDiv, programIndexH1, infoLine1Div));
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
