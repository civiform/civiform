package views.admin.programs;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ul;

import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.Styles;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public ProgramBlockEditView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request, ProgramDefinition program, BlockDefinition block) {
    return layout.render(
        div().with(
            a().withText("Add block")
                .withHref(
                    controllers.admin.routes.AdminProgramBlocksController.create(program.id())
                        .url()))
    .with(blockOrderPanel(program, block)));
  }

  public ContainerTag blockOrderPanel(ProgramDefinition program, BlockDefinition focusedBlock) {
    ContainerTag list = ul();
    for (BlockDefinition block : program.blockDefinitions()) {
      ContainerTag link = a().withText(block.name()).withHref(controllers.admin.routes.AdminProgramBlocksController.edit(program.id(), block.id()).url());
      if (block.equals(focusedBlock)) {
        link.withClass(Styles.FONT_BOLD);
      }

      list.with(li().with(link));
    }
    return list;
  }
}
