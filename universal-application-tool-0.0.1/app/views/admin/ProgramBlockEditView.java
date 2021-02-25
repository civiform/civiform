package views.admin;

import com.google.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import views.BaseHtmlView;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminProgramLayout layout;

  @Inject
  public ProgramBlockEditView(AdminProgramLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program) {
    return layout.render();
  }


  public Content render(Request request, ProgramDefinition program, BlockDefinition block) {
    return layout.render();
  }
}
