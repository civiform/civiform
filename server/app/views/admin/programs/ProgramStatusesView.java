package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;

public final class ProgramStatusesView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramStatusesView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render() {
    HtmlBundle htmlBundle =
        layout.getBundle().setTitle("TODO(#XYZ)").addMainContent(p("hello world"));

    return layout.renderCentered(htmlBundle);
  }
}
