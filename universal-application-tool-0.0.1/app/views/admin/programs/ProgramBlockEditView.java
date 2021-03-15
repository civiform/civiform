package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.main;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;
import static views.ViewUtils.POST;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.QuestionDefinition;
import views.BaseHtmlView;
import views.StyleUtils;
import views.Styles;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.QuestionBank;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  private static final String CREATE_BLOCK_FORM_ID = "block-create";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete";

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

    ContainerTag mainContent =
        main(
            addFormEndpoints(csrfTag, program.id(), block.id()),
            programInfo(program),
            div()
                .withId("program-block-info")
                .withClasses(Styles.FLEX, Styles.FLEX_GROW, Styles._MX_2)
                .with(blockOrderPanel(program, block))
                .with(blockEditPanel(program, block, csrfTag))
                .with(questionBankPanel(questions, program, block, csrfTag)));

    return layout.renderMain(mainContent, Styles.FLEX, Styles.FLEX_COL);
  }

  private Tag addFormEndpoints(Tag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    ContainerTag createBlockForm =
        form(csrfTag).withId(CREATE_BLOCK_FORM_ID).withMethod(POST).withAction(blockCreateAction);

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    ContainerTag deleteBlockForm =
        form(csrfTag).withId(DELETE_BLOCK_FORM_ID).withMethod(POST).withAction(blockDeleteAction);

    return div(createBlockForm, deleteBlockForm).withClasses(Styles.HIDDEN);
  }

  private Tag programInfo(ProgramDefinition program) {
    ContainerTag programStatus =
        div("Draft").withId("program-status").withClasses(Styles.TEXT_XS, Styles.UPPERCASE);
    ContainerTag programTitle =
        div(program.name()).withId("program-title").withClasses(Styles.TEXT_3XL, Styles.PB_3);
    ContainerTag programDescription = div(program.description()).withClasses(Styles.TEXT_SM);

    ContainerTag programInfo =
        div(programStatus, programTitle, programDescription)
            .withId("program-info")
            .withClasses(
                Styles.BG_GRAY_100,
                Styles.TEXT_GRAY_800,
                Styles.SHADOW_MD,
                Styles.P_8,
                Styles.PT_4,
                Styles._MX_2);

    return programInfo;
  }

  public ContainerTag blockOrderPanel(ProgramDefinition program, BlockDefinition focusedBlock) {
    ContainerTag ret =
        div()
            .withClasses(
                Styles.SHADOW_LG,
                Styles.PT_6,
                Styles.W_1_5,
                Styles.BORDER_R,
                Styles.BORDER_GRAY_200);

    for (BlockDefinition block : program.blockDefinitions()) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(program.id(), block.id())
              .url();

      // TODO: Not i18n safe.
      int numQuestions = block.getQuestionCount();
      String questionCountText = String.format("%d Question", numQuestions);
      if (numQuestions != 1) {
        questionCountText += 's';
      }
      String blockName = block.name();

      ContainerTag blockTag =
          a().withHref(editBlockLink)
              .with(p(blockName), p(questionCountText).withClasses(Styles.TEXT_SM));
      String selectedClasses = block.hasSameId(focusedBlock) ? Styles.BG_GRAY_100 : "";
      blockTag.withClasses(Styles.BLOCK, Styles.PY_2, Styles.PX_4, selectedClasses);
      ret.with(blockTag);
    }

    ret.with(
        submitButton("Add Block")
            .withId("add-block-button")
            .attr(Attr.FORM, CREATE_BLOCK_FORM_ID)
            .withClasses(Styles.M_4));
    return ret;
  }

  private ContainerTag blockEditPanel(
      ProgramDefinition program, BlockDefinition block, Tag csrfTag) {
    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(program.id(), block.id())
            .url();
    ContainerTag blockInfoForm = form(csrfTag).withMethod(POST).withAction(blockUpdateAction);

    blockInfoForm.with(
        FieldWithLabel.input()
            .setId("name")
            .setLabelText("Block name")
            .setValue(block.name())
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("description")
            .setLabelText("Block description")
            .setValue(block.description())
            .getContainer(),
        submitButton("Update Block").withClasses(Styles.MX_4, Styles.MY_1, Styles.INLINE));
    if (program.blockDefinitions().size() > 1) {
      blockInfoForm.with(
          submitButton("Delete Block")
              .withId("delete-block-button")
              .attr(Attr.FORM, DELETE_BLOCK_FORM_ID)
              .withClasses(Styles.MX_4, Styles.MY_1, Styles.INLINE));
    }

    String deleteQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.destroy(
                program.id(), block.id())
            .url();
    ContainerTag questionDeleteForm =
        form(csrfTag).withId("blockQuestions").withMethod(POST).withAction(deleteQuestionAction);
    block
        .programQuestionDefinitions()
        .forEach(pqd -> questionDeleteForm.with(renderQuestion(pqd.getQuestionDefinition())));

    return div().withClasses(Styles.FLEX_AUTO, Styles.PY_6).with(blockInfoForm, questionDeleteForm);
  }

  public ContainerTag renderQuestion(QuestionDefinition definition) {
    ContainerTag ret =
        div()
            .withClasses(
                Styles.RELATIVE,
                Styles.MX_4,
                Styles.MY_2,
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.PX_4,
                Styles.PY_2,
                Styles.FLEX,
                Styles.ITEMS_START,
                StyleUtils.hover(Styles.TEXT_GRAY_800, Styles.BG_GRAY_100));

    Tag removeButton =
        TagCreator.button(text(definition.getName()))
            .withType("submit")
            .withId("block-question-" + definition.getId())
            .withName("block-question-" + definition.getId())
            .withValue(definition.getId() + "")
            .withClasses(
                Styles.OPACITY_0,
                Styles.ABSOLUTE,
                Styles.LEFT_0,
                Styles.TOP_0,
                Styles.W_FULL,
                Styles.H_FULL);

    ContainerTag icon =
        Icons.questionTypeSvg(definition.getQuestionType(), 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    ContainerTag content =
        div()
            .withClasses(Styles.ML_4)
            .with(
                p(definition.getName()),
                p(definition.getDescription()).withClasses(Styles.MT_1, Styles.TEXT_SM),
                removeButton);
    return ret.with(icon, content);
  }

  private ContainerTag questionBankPanel(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition block,
      Tag csrfTag) {
    String addQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                program.id(), block.id())
            .url();

    QuestionBank qb =
        new QuestionBank()
            .setQuestionAction(addQuestionAction)
            .setCsrfTag(csrfTag)
            .setQuestions(questionDefinitions)
            .setProgram(program);

    return qb.getContainer();
  }
}
