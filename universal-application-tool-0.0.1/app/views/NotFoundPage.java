package views;

import static j2html.TagCreator.h1;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import play.twirl.api.Content;
import views.style.Styles;

public class NotFoundPage extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  private final Config config;

  @Inject
  public NotFoundPage(BaseHtmlLayout layout, Config config) {
    this.layout = layout;
    this.config = config;
  }

  public Content render() {

    HtmlBundle htmlBundle =
        this.layout
            .getBundle()
            .setTitle("404 Error")
            .addBodyStyles(Styles.P_4)
            .addMainContent(h1("404 Error"));

    return layout.render(htmlBundle);
  }
}
