package views.dev;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.title;

import controllers.dev.routes;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;

public class DatabaseSeedView extends BaseHtmlView {
  private final BaseHtmlLayout layout;

  @Inject
  public DatabaseSeedView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render() {
    return layout.htmlContent(
        head(title("Seed database")),
        body()
            .with(h1("Seed the database with mock content by clicking below"))
            .with(a("Seed the database").withHref(routes.DatabaseSeedController.seed().url())));
  }
}
