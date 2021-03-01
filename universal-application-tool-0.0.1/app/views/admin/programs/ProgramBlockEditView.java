package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import views.BaseHtmlView;
import views.Styles;
import views.admin.AdminLayout;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public ProgramBlockEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Request request,
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> questions) {
    Tag csrfTag = makeCsrfTokenInputTag(request);
    System.out.println(csrfTag.render());
    return layout.render(
        title(program),
        topButtons(request, program),
        div()
            .withClass(Styles.FLEX)
            .with(blockOrderPanel(program, block))
            .with(blockEditPanel(request, program, block))
            .with(questionBankPanel(questions)));
  }

  private Tag title(ProgramDefinition program) {
    return h1(program.name() + " Questions");
  }

  private ContainerTag topButtons(Request request, ProgramDefinition program) {
    String addBlockUrl =
        controllers.admin.routes.AdminProgramBlocksController.create(program.id()).url();
    ContainerTag addBlockButton =
        form(makeCsrfTokenInputTag(request), submitButton("Add Block"))
            .withMethod("post")
            .withAction(addBlockUrl);

    return div(addBlockButton);
  }

  private ContainerTag blockOrderPanel(ProgramDefinition program, BlockDefinition focusedBlock) {
    ContainerTag list = ul();

    for (BlockDefinition block : program.blockDefinitions()) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(program.id(), block.id())
              .url();
      ContainerTag link = a().withText(block.name()).withHref(editBlockLink);

      if (block.equals(focusedBlock)) {
        link.withClass(Styles.FONT_BOLD);
      }

      list.with(li(link));
    }

    return div().withClasses(Styles.FLEX_INITIAL).with(list);
  }

  private ContainerTag blockEditPanel(
      Request request, ProgramDefinition program, BlockDefinition block) {
    return div().withClass(Styles.FLEX_AUTO).with(blockEditPanelTop(request, program, block));
  }

  private ContainerTag blockEditPanelTop(
      Request request, ProgramDefinition program, BlockDefinition block) {
    String deleteBlockLink =
        controllers.admin.routes.AdminProgramBlocksController.destroy(program.id(), block.id())
            .url();
    ContainerTag deleteButton =
        form(makeCsrfTokenInputTag(request), submitButton("Delete Block"))
            .withMethod("post")
            .withAction(deleteBlockLink);
    return div().withClass(Styles.FLEX).with(h1(block.name())).with(deleteButton);
  }

  private ContainerTag questionBankPanel(ImmutableList<QuestionDefinition> questionDefinitions) {
    ContainerTag questionList = ul().withClass(Styles.OVERFLOW_X_SCROLL);

    questionDefinitions.forEach(
        questionDefinition -> questionList.with(li(questionDefinition.getName())));

    return div().withClasses(Styles.FLEX_INITIAL).with(h2("Question Bank")).with(questionList);
  }
}
