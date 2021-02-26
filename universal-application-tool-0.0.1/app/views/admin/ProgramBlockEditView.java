package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import views.BaseHtmlView;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public ProgramBlockEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program, BlockDefinition block) {
    return layout.render(
        div(
            a().withText("Add block")
                .withHref(
                    controllers.admin.routes.AdminProgramBlocksController.create(program.id())
                        .url())));
  }
}
