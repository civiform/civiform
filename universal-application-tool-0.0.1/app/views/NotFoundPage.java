package views;

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

public class NotFoundPage extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public NotFoundPage(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render() {
    String title = "Not Found";

    HtmlBundle htmlBundle = this.layout.getBundle().setTitle(title);
    htmlBundle.addMainContent(mainContent());

    return layout.render(htmlBundle);
  }

  private ContainerTag mainContent() {
    ContainerTag content = div();

    /*content.with(
        this.layout
            .viewUtils
            .makeLocalImageTag("ChiefSeattle_Blue")
            .withClasses(Styles.W_1_4, Styles.PT_4));
    content.with(
        div()
            .withClasses(Styles.FLEX, Styles.TEXT_4XL, Styles.GAP_1, Styles._MT_6, Styles.PX_8)
            .with(p("Seattle").withClasses(Styles.FONT_BOLD))
            .with(p("CiviForm")));

    content.with(
        div()
            .withClasses(
                Styles.FLEX,
                Styles.FLEX_COL,
                Styles.GAP_2,
                Styles.PY_6,
                Styles.PX_8,
                Styles.TEXT_LG,
                Styles.W_FULL,
                Styles.PLACE_ITEMS_CENTER));

    content.with(
        div()
            .withClasses(
                Styles.BG_GRAY_100,
                Styles.PY_4,
                Styles.PX_8,
                Styles.W_FULL,
                Styles.FLEX,
                Styles.GAP_2,
                Styles.JUSTIFY_CENTER,
                Styles.ITEMS_CENTER,
                Styles.TEXT_BASE)
            .with(text(" ")));*/

    return div()
        .with(content);
  }
}
