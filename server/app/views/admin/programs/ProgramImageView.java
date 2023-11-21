package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.BaseHtmlView.renderHeader;

import com.google.inject.Inject;
import j2html.tags.specialized.H1Tag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

/** A view for admins to update the image associated with a particular program. */
public final class ProgramImageView {
  private final AdminLayout layout;

  @Inject
  public ProgramImageView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   *
   * <p>TODO(#5676): Implement the forms to add an image and alt text.
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    String title =
        String.format(
            "Manage program image for %s", programDefinition.localizedName().getDefault());
    H1Tag headerDiv = renderHeader(title, "my-10", "mx-10");
    HtmlBundle htmlBundle = layout.getBundle(request).setTitle(title).addMainContent(headerDiv);
    return layout.renderCentered(htmlBundle);
  }
}
