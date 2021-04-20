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
import forms.BlockForm;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.OptionalLong;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.QuestionBank;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;

  public static final String REPEATER_ID_FORM_FIELD = "repeaterId";
  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";

  @Inject
  public ProgramBlockEditView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Request request,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      String message,
      ImmutableList<QuestionDefinition> questions) {
    return render(
        request,
        program,
        blockDefinition.id(),
        new BlockForm(blockDefinition.name(), blockDefinition.description()),
        blockDefinition,
        blockDefinition.programQuestionDefinitions(),
        message,
        questions);
  }

  public Content render(
      Request request,
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      String message,
      ImmutableList<QuestionDefinition> questions) {
    Tag csrfTag = makeCsrfTokenInputTag(request);

    ContainerTag mainContent =
        main(
            addFormEndpoints(csrfTag, program.id(), blockId),
            programInfo(program),
            div()
                .withId("program-block-info")
                .withClasses(Styles.FLEX, Styles.FLEX_GROW, Styles._MX_2)
                .with(blockOrderPanel(program, blockId))
                .with(
                    blockEditPanel(
                        program,
                        blockId,
                        blockForm,
                        blockQuestions,
                        blockDefinition.isRepeater(),
                        csrfTag))
                .with(questionBankPanel(questions, program, blockDefinition, csrfTag)));

    if (message.length() > 0) {
      mainContent.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }

    return layout.renderCentered(mainContent, Styles.FLEX, Styles.FLEX_COL);
  }

  private Tag addFormEndpoints(Tag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    ContainerTag createBlockForm =
        form(csrfTag).withId(CREATE_BLOCK_FORM_ID).withMethod(POST).withAction(blockCreateAction);

    ContainerTag createRepeatedBlockForm =
        form(csrfTag)
            .withId(CREATE_REPEATED_BLOCK_FORM_ID)
            .withMethod(POST)
            .withAction(blockCreateAction)
            .with(
                FieldWithLabel.number()
                    .setFieldName(REPEATER_ID_FORM_FIELD)
                    .setValue(OptionalLong.of(blockId))
                    .getContainer());

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    ContainerTag deleteBlockForm =
        form(csrfTag).withId(DELETE_BLOCK_FORM_ID).withMethod(POST).withAction(blockDeleteAction);

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm)
        .withClasses(Styles.HIDDEN);
  }

  private Tag programInfo(ProgramDefinition program) {
    ContainerTag programStatus =
        div("Draft").withId("program-status").withClasses(Styles.TEXT_XS, Styles.UPPERCASE);
    ContainerTag programTitle =
        div(program.adminName()).withId("program-title").withClasses(Styles.TEXT_3XL, Styles.PB_3);
    ContainerTag programDescription = div(program.adminDescription()).withClasses(Styles.TEXT_SM);

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

  public ContainerTag blockOrderPanel(ProgramDefinition program, long focusedBlockId) {
    ContainerTag ret =
        div()
            .withClasses(
                Styles.SHADOW_LG,
                Styles.PT_6,
                Styles.W_1_5,
                Styles.BORDER_R,
                Styles.BORDER_GRAY_200);
    ret.with(
        renderBlockList(program, program.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));
    ret.with(
        submitButton("Add Block")
            .withId("add-block-button")
            .attr(Attr.FORM, CREATE_BLOCK_FORM_ID)
            .withClasses(Styles.M_4));
    return ret;
  }

  private ContainerTag renderBlockList(
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level) {
    ContainerTag container = div().withClass("pl-" + level * 2);
    for (BlockDefinition blockDefinition : blockDefinitions) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(
                  programDefinition.id(), blockDefinition.id())
              .url();

      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      String blockName = blockDefinition.name();

      ContainerTag blockTag =
          a().withHref(editBlockLink)
              .with(p(blockName), p(questionCountText).withClasses(Styles.TEXT_SM));
      String selectedClasses = blockDefinition.id() == focusedBlockId ? Styles.BG_GRAY_100 : "";
      blockTag.withClasses(Styles.BLOCK, Styles.PY_2, Styles.PX_4, selectedClasses);

      container.with(blockTag);

      // Recursively add repeated blocks indented under their repeater block
      if (blockDefinition.isRepeater()) {
        container.with(
            renderBlockList(
                programDefinition,
                programDefinition.getBlockDefinitionsForRepeater(blockDefinition.id()),
                focusedBlockId,
                level + 1));
      }
    }
    return container;
  }

  private ContainerTag blockEditPanel(
      ProgramDefinition program,
      long blockId,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      boolean blockDefinitionIsRepeater,
      Tag csrfTag) {
    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(program.id(), blockId).url();

    ContainerTag blockInfoForm = form(csrfTag).withMethod(POST).withAction(blockUpdateAction);

    blockInfoForm.with(
        FieldWithLabel.input()
            .setId("block-name-input")
            .setFieldName("name")
            .setLabelText("Block name")
            .setValue(blockForm.getName())
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("block-description-textarea")
            .setFieldName("description")
            .setLabelText("Block description")
            .setValue(blockForm.getDescription())
            .getContainer(),
        submitButton("Update Block")
            .withId("update-block-button")
            .withClasses(Styles.MX_4, Styles.MY_1, Styles.INLINE));

    if (program.blockDefinitions().size() > 1) {
      blockInfoForm.with(
          submitButton("Delete Block")
              .withId("delete-block-button")
              .attr(Attr.FORM, DELETE_BLOCK_FORM_ID)
              .withClasses(Styles.MX_4, Styles.MY_1, Styles.INLINE));
    }

    if (blockDefinitionIsRepeater) {
      blockInfoForm.with(
          submitButton("Create Repeated Block")
              .withId("create-repeated-block-button")
              .attr(Attr.FORM, CREATE_REPEATED_BLOCK_FORM_ID)
              .withClasses(Styles.MX_4, Styles.MY_1, Styles.INLINE));
    }

    String deleteQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.destroy(program.id(), blockId)
            .url();
    ContainerTag questionDeleteForm =
        form(csrfTag)
            .withId("block-questions-form")
            .withMethod(POST)
            .withAction(deleteQuestionAction);
    blockQuestions.forEach(
        pqd -> questionDeleteForm.with(renderQuestion(pqd.getQuestionDefinition())));

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
            .withClasses(ReferenceClasses.REMOVE_QUESTION_BUTTON, BaseStyles.CLICK_TARGET_BUTTON);

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
      BlockDefinition blockDefinition,
      Tag csrfTag) {
    String addQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                program.id(), blockDefinition.id())
            .url();

    QuestionBank qb =
        new QuestionBank()
            .setQuestionAction(addQuestionAction)
            .setCsrfTag(csrfTag)
            .setQuestions(questionDefinitions)
            .setProgram(program)
            .setBlockDefinition(blockDefinition);
    return qb.getContainer();
  }
}
