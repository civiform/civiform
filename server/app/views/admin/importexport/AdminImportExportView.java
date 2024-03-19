package views.admin.importexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

public final class AdminImportExportView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public AdminImportExportView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT_EXPORT);
  }

  public Content render(Http.Request request) {
    String title = "Import and export questions and programs";
    DivTag contentDiv = div().with(h1(title));

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }
}
