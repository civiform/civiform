package views.errors;

import javax.inject.Inject;
import play.twirl.api.Content;
import static j2html.TagCreator.h1;
import views.BaseHtmlView;
import views.BaseHtmlLayout;
import static com.google.common.base.Preconditions.checkNotNull;

public class NotFound extends BaseHtmlView {

  private final BaseHtmlLayout layout;
  
  @Inject
  public NotFound(BaseHtmlLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render() {

    return layout
        .getBundle()
        .setTitle("Page Not Found")
        .addMainContent(h1("This page does not exist on Civiform"))
        .render();
  }
}
