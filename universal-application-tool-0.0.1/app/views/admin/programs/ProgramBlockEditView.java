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
import views.components.FieldWithLabel;

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

    return layout.render(
        title(program),
        topButtons(csrfTag, program),
        div()
            .withClasses(Styles.FLEX)
            .with(blockOrderPanel(program, block))
            .with(blockEditPanel(csrfTag, program, block))
            .with(questionBankPanel(csrfTag, questions, program, block)));
  }

  private Tag title(ProgramDefinition program) {
    return h1(program.name() + " Questions");
  }

  private ContainerTag topButtons(Tag csrfTag, ProgramDefinition program) {
    String addBlockUrl =
        controllers.admin.routes.AdminProgramBlocksController.create(program.id()).url();
    ContainerTag addBlockForm =
        form(csrfTag, submitButton("Add Block")).withMethod("post").withAction(addBlockUrl);

    return div(addBlockForm);
  }

  private ContainerTag blockOrderPanel(ProgramDefinition program, BlockDefinition focusedBlock) {
    ContainerTag list = ul();

    for (BlockDefinition block : program.blockDefinitions()) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(program.id(), block.id())
              .url();
      ContainerTag link = a().withText(block.name()).withHref(editBlockLink);

      if (block.hasSameId(focusedBlock)) {
        link.withClass(Styles.FONT_BOLD);
      }

      list.with(li(link));
    }

    return div().withClasses(Styles.FLEX_INITIAL).with(list);
  }

  private ContainerTag blockEditPanel(
      Tag csrfTag, ProgramDefinition program, BlockDefinition block) {
    ContainerTag questionList = ul().withId("blockQuestions");
    block
        .programQuestionDefinitions()
        .forEach(
            pqd ->
                questionList.with(
                    li(
                        checkboxInputWithLabel(
                            pqd.getQuestionDefinition().getName(),
                            "block-question-" + pqd.getQuestionDefinition().getId(),
                            "block-question-" + pqd.getQuestionDefinition().getId(),
                            String.valueOf(pqd.getQuestionDefinition().getId())))));

    return div()
        .withClass(Styles.FLEX_AUTO)
        .with(blockEditPanelTop(csrfTag, program, block))
        .with(
            div(
                form(
                        csrfTag,
                        FieldWithLabel.createInput("name")
                            .setLabelText("Block name")
                            .setValue(block.name())
                            .getContainer(),
                        FieldWithLabel.createTextArea("description")
                            .setLabelText("Block description")
                            .setValue(block.description())
                            .getContainer(),
                        submitButton("Update Block"))
                    .withMethod("post")
                    .withAction(
                        controllers.admin.routes.AdminProgramBlocksController.update(
                                program.id(), block.id())
                            .url()),
                form()
                    .withMethod("post")
                    .withAction(
                        controllers.admin.routes.AdminProgramBlockQuestionsController.destroy(
                                program.id(), block.id())
                            .url())
                    .with(csrfTag)
                    .with(questionList)
                    .with(submitButton("Remove questions"))));
  }

  private ContainerTag blockEditPanelTop(
      Tag csrfTag, ProgramDefinition program, BlockDefinition block) {
    String deleteBlockLink =
        controllers.admin.routes.AdminProgramBlocksController.destroy(program.id(), block.id())
            .url();

    ContainerTag blockEditPanelTop = div().withClass(Styles.FLEX).with(h1(block.name()));
    if (program.blockDefinitions().size() > 1) {
      ContainerTag deleteButton =
          form(csrfTag, submitButton("Delete Block"))
              .withMethod("post")
              .withAction(deleteBlockLink);
      blockEditPanelTop.with(deleteButton);
    }
    return blockEditPanelTop;
  }

  private ContainerTag questionBankPanel(
      Tag csrfTag,
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition block) {
    ContainerTag questionBank = div().withClasses(Styles.FLEX_INITIAL).with(h2("Question Bank"));
    ContainerTag form =
        form()
            .withMethod("post")
            .withAction(
                controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                        program.id(), block.id())
                    .url())
            .with(csrfTag)
            .with(submitButton("Add to Block"));

    ContainerTag questionList =
        ul().withId("questionBankQuestions").withClass(Styles.OVERFLOW_X_SCROLL);

    questionDefinitions.stream()
        .filter(question -> !program.hasQuestion(question))
        .forEach(
            questionDefinition ->
                questionList.with(
                    li(
                        checkboxInputWithLabel(
                            questionDefinition.getName(),
                            "question-" + questionDefinition.getId(),
                            "question-" + questionDefinition.getId(),
                            String.valueOf(questionDefinition.getId())))));

    return questionBank.with(form.with(questionList));
  }
}
