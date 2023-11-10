package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;

import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

/** A view for admins to update the image associated with a particular program. */
public class ProgramImageView {
  private final AdminLayout layout;

  @Inject
  public ProgramImageView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.PROGRAMS);
  }

  /**
   * Renders the image currently associated with the program and a form to add / edit / delete the
   * image (and its alt text).
   */
  public Content render(Http.Request request, ProgramDefinition programDefinition) {
    DivTag headingDiv =
        div()
            .withClasses("flex", "items-center", "space-x-4", "mt-12", "mb-10")
            .with(
                h1(
                    String.format(
                        "Manage program image: %s",
                        programDefinition.localizedName().getDefault())),
                div().withClass("flex-grow"));
    HtmlBundle htmlBundle =
        layout.getBundle(request).setTitle("Manage program image").addMainContent(headingDiv);
    return layout.renderCentered(htmlBundle);
  }
}
