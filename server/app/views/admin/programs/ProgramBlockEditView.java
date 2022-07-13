package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.Optional;
import java.util.OptionalLong;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.QuestionBank;
import views.components.SvgTag;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for an admin to edit the configuration for a single block of a program. */
public class ProgramBlockEditView extends BaseHtmlView {

  private final AdminLayout layout;
  private final boolean featureFlagOptionalQuestions;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";

  @Inject
  public ProgramBlockEditView(AdminLayoutFactory layoutFactory, Config config) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.featureFlagOptionalQuestions = checkNotNull(config).hasPath("cf.optional_questions");
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
      ProgramDefinition programDefinition,
      long blockId,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      String message,
      ImmutableList<QuestionDefinition> questions) {
    InputTag csrfTag = makeCsrfTokenInputTag(request);
    String title = String.format("Edit %s", blockDefinition.name());

    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(
                programDefinition.id(), blockId)
            .url();
    Modal blockDescriptionEditModal = blockDescriptionModal(csrfTag, blockForm, blockUpdateAction);

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(Styles.FLEX, Styles.FLEX_COL)
            .addMainContent(
                addFormEndpoints(csrfTag, programDefinition.id(), blockId),
                layout.renderProgramInfo(programDefinition),
                div()
                    .withId("program-block-info")
                    .withClasses(Styles.FLEX, Styles.FLEX_GROW, Styles._MX_2)
                    .with(blockOrderPanel(request, programDefinition, blockId))
                    .with(
                        blockEditPanel(
                            programDefinition,
                            blockDefinition,
                            blockForm,
                            blockQuestions,
                            questions,
                            blockDefinition.isEnumerator(),
                            csrfTag,
                            blockDescriptionEditModal.getButton()))
                    .with(
                        questionBankPanel(questions, programDefinition, blockDefinition, csrfTag)))
            .addModals(blockDescriptionEditModal);

    // Add toast messages
    if (request.flash().get("error").isPresent()) {
      htmlBundle.addToastMessages(
          ToastMessage.error(request.flash().get("error").get()).setDuration(-1));
    }
    if (message.length() > 0) {
      htmlBundle.addToastMessages(ToastMessage.error(message).setDismissible(false));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag addFormEndpoints(InputTag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    FormTag createBlockForm =
        form(csrfTag)
            .withId(CREATE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction);

    FormTag createRepeatedBlockForm =
        form(csrfTag)
            .withId(CREATE_REPEATED_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction)
            .with(
                FieldWithLabel.number()
                    .setFieldName(ENUMERATOR_ID_FORM_FIELD)
                    .setScreenReaderText(ENUMERATOR_ID_FORM_FIELD)
                    .setValue(OptionalLong.of(blockId))
                    .getNumberTag());

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    FormTag deleteBlockForm =
        form(csrfTag)
            .withId(DELETE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockDeleteAction);

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm)
        .withClasses(Styles.HIDDEN);
  }

  private DivTag blockOrderPanel(Request request, ProgramDefinition program, long focusedBlockId) {
    DivTag ret =
        div()
            .withClasses(
                Styles.SHADOW_LG,
                Styles.PT_6,
                Styles.W_1_5,
                Styles.BORDER_R,
                Styles.BORDER_GRAY_200);
    ret.with(
        renderBlockList(
            request, program, program.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));
    ret.with(
        submitButton("Add Screen")
            .withId("add-block-button")
            .withForm(CREATE_BLOCK_FORM_ID)
            .withClasses(Styles.M_4));
    return ret;
  }

  private DivTag renderBlockList(
      Request request,
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level) {
    DivTag container = div().withClass("pl-" + level * 2);
    for (BlockDefinition blockDefinition : blockDefinitions) {
      String editBlockLink =
          controllers.admin.routes.AdminProgramBlocksController.edit(
                  programDefinition.id(), blockDefinition.id())
              .url();

      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      String blockName = blockDefinition.name();

      DivTag moveButtons =
          blockMoveButtons(request, programDefinition.id(), blockDefinitions, blockDefinition);
      String selectedClasses = blockDefinition.id() == focusedBlockId ? Styles.BG_GRAY_100 : "";
      DivTag blockTag =
          div()
              .withClasses(
                  Styles.FLEX,
                  Styles.FLEX_ROW,
                  Styles.GAP_2,
                  Styles.PY_2,
                  Styles.PX_4,
                  Styles.BORDER,
                  Styles.BORDER_WHITE,
                  StyleUtils.hover(Styles.BORDER_GRAY_300),
                  selectedClasses)
              .with(
                  a().withClasses(Styles.FLEX_GROW, Styles.OVERFLOW_HIDDEN)
                      .withHref(editBlockLink)
                      .with(p(blockName), p(questionCountText).withClasses(Styles.TEXT_SM)))
              .with(moveButtons);

      container.with(blockTag);

      // Recursively add repeated blocks indented under their enumerator block
      if (blockDefinition.isEnumerator()) {
        container.with(
            renderBlockList(
                request,
                programDefinition,
                programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id()),
                focusedBlockId,
                level + 1));
      }
    }
    return container;
  }

  private DivTag blockMoveButtons(
      Request request,
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {
    String moveUpFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? Styles.INVISIBLE : "";
    DivTag moveUp =
        div()
            .withClass(moveUpInvisible)
            .with(
                form()
                    .withAction(moveUpFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.UP.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));

    String moveDownFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? Styles.INVISIBLE
            : "";
    DivTag moveDown =
        div()
            .withClasses(Styles.TRANSFORM, Styles.ROTATE_180, moveDownInvisible)
            .with(
                form()
                    .withAction(moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.DOWN.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    DivTag moveButtons =
        div().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.SELF_CENTER).with(moveUp, moveDown);
    return moveButtons;
  }

  private DivTag blockEditPanel(
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean blockDefinitionIsEnumerator,
      InputTag csrfTag,
      ButtonTag blockDescriptionModalButton) {
    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canDelete =
        !blockDefinitionIsEnumerator || hasNoRepeatedBlocks(program, blockDefinition.id());

    DivTag blockInfoDisplay =
        div()
            .with(
                div(blockForm.getName()).withClasses(Styles.TEXT_XL, Styles.FONT_BOLD, Styles.PY_2))
            .with(div(blockForm.getDescription()).withClasses(Styles.TEXT_LG, Styles.MAX_W_PROSE))
            .withClasses(Styles.M_4);

    DivTag predicateDisplay =
        renderPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.visibilityPredicate(),
            blockDefinition.name(),
            allQuestions);

    // Add buttons to change the block.
    DivTag buttons = div().withClasses(Styles.MX_4, Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_4);
    buttons.with(blockDescriptionModalButton);
    if (blockDefinitionIsEnumerator) {
      buttons.with(
          submitButton("Create Repeated Screen")
              .withId("create-repeated-block-button")
              .withForm(CREATE_REPEATED_BLOCK_FORM_ID));
    }
    // TODO: Maybe add alpha variants to button color on hover over so we do not have
    //  to hard-code what the color will be when button is in hover state?
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass(Styles.FLEX_GROW));
      buttons.with(
          submitButton("Delete Screen")
              .withId("delete-block-button")
              .withForm(DELETE_BLOCK_FORM_ID)
              .withCondDisabled(!canDelete)
              .withCondTitle(
                  !canDelete, "A screen can only be deleted when it has no repeated screens.")
              .withClasses(
                  Styles.MX_4,
                  Styles.MY_1,
                  Styles.BG_RED_500,
                  StyleUtils.hover(Styles.BG_RED_700),
                  Styles.INLINE,
                  StyleUtils.disabled(Styles.OPACITY_50)));
    }

    DivTag programQuestions = div();
    blockQuestions.forEach(
        pqd ->
            programQuestions.with(
                renderQuestion(
                    csrfTag,
                    program.id(),
                    blockDefinition.id(),
                    pqd.getQuestionDefinition(),
                    canDelete,
                    pqd.optional())));

    return div()
        .withClasses(Styles.FLEX_AUTO, Styles.PY_6)
        .with(blockInfoDisplay, buttons, predicateDisplay, programQuestions);
  }

  private DivTag renderPredicate(
      long programId,
      long blockId,
      Optional<PredicateDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions) {
    String currentBlockStatus =
        predicate.isEmpty()
            ? "This screen is always shown."
            : predicate.get().toDisplayString(blockName, questions);
    return div()
        .withClasses(Styles.M_4)
        .with(
            div("Visibility condition").withClasses(Styles.TEXT_LG, Styles.FONT_BOLD, Styles.PY_2))
        .with(div(currentBlockStatus).withClasses(Styles.TEXT_LG, Styles.MAX_W_PROSE))
        .with(
            redirectButton(
                ReferenceClasses.EDIT_PREDICATE_BUTTON,
                "Edit visibility condition",
                routes.AdminProgramBlockPredicatesController.edit(programId, blockId).url()));
  }

  private DivTag renderQuestion(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove,
      boolean isOptional) {
    DivTag ret =
        div()
            .withClasses(
                ReferenceClasses.PROGRAM_QUESTION,
                Styles.MX_4,
                Styles.MY_2,
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.PX_4,
                Styles.PY_2,
                Styles.FLEX,
                Styles.GAP_4,
                Styles.ITEMS_CENTER,
                StyleUtils.hover(Styles.TEXT_GRAY_800, Styles.BG_GRAY_100));

    SvgTag icon =
        Icons.questionTypeSvg(questionDefinition.getQuestionType(), 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    DivTag content =
        div()
            .withClass(Styles.FLEX_GROW)
            .with(p(questionDefinition.getName()))
            .with(p(questionDefinition.getDescription()).withClasses(Styles.MT_1, Styles.TEXT_SM));

    Optional<FormTag> maybeOptionalToggle =
        optionalToggle(
            csrfTag, programDefinitionId, blockDefinitionId, questionDefinition, isOptional);

    ret.with(icon, content);
    if (maybeOptionalToggle.isPresent()) {
      ret.with(maybeOptionalToggle.get());
    }
    return ret.with(
        deleteQuestionForm(
            csrfTag, programDefinitionId, blockDefinitionId, questionDefinition, canRemove));
  }

  private Optional<FormTag> optionalToggle(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean isOptional) {
    if (!featureFlagOptionalQuestions) {
      return Optional.empty();
    }
    if (questionDefinition instanceof StaticContentQuestionDefinition) {
      return Optional.empty();
    }
    ButtonTag optionalButton =
        TagCreator.button()
            .withClasses(
                Styles.FLEX,
                Styles.GAP_2,
                Styles.ITEMS_CENTER,
                isOptional ? Styles.TEXT_BLACK : Styles.TEXT_GRAY_400,
                Styles.FONT_MEDIUM,
                Styles.BG_TRANSPARENT,
                Styles.ROUNDED_FULL,
                StyleUtils.hover(Styles.BG_GRAY_400, Styles.TEXT_GRAY_300))
            .withType("submit")
            .with(p("optional").withClasses("hover-group:text-white"))
            .with(
                div()
                    .withClasses(Styles.RELATIVE)
                    .with(
                        div()
                            .withClasses(
                                isOptional ? Styles.BG_BLUE_600 : Styles.BG_GRAY_600,
                                Styles.W_14,
                                Styles.H_8,
                                Styles.ROUNDED_FULL))
                    .with(
                        div()
                            .withClasses(
                                Styles.ABSOLUTE,
                                Styles.BG_WHITE,
                                isOptional ? Styles.RIGHT_1 : Styles.LEFT_1,
                                Styles.TOP_1,
                                Styles.W_6,
                                Styles.H_6,
                                Styles.ROUNDED_FULL)));
    String toggleOptionalAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.setOptional(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
    return Optional.of(
        form(csrfTag)
            .withMethod(HttpVerbs.POST)
            .withAction(toggleOptionalAction)
            .with(input().isHidden().withName("optional").withValue(isOptional ? "false" : "true"))
            .with(optionalButton));
  }

  private FormTag deleteQuestionForm(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove) {
    ButtonTag removeButton =
        TagCreator.button(text("DELETE"))
            .withType("submit")
            .withId("block-question-" + questionDefinition.getId())
            .withName("questionDefinitionId")
            .withValue(String.valueOf(questionDefinition.getId()))
            .withCondDisabled(!canRemove)
            .withCondTitle(
                !canRemove,
                "An enumerator question can only be removed from the screen when the screen has no"
                    + " repeated screens.")
            .withClasses(
                ReferenceClasses.REMOVE_QUESTION_BUTTON, canRemove ? "" : Styles.OPACITY_50);
    String deleteQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.destroy(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
    return form(csrfTag)
        .withId("block-questions-form")
        .withMethod(HttpVerbs.POST)
        .withAction(deleteQuestionAction)
        .with(removeButton);
  }

  private FormTag questionBankPanel(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      InputTag csrfTag) {
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

  private Modal blockDescriptionModal(
      InputTag csrfTag, BlockForm blockForm, String blockUpdateAction) {
    String modalTitle = "Screen Name and Description";
    String modalButtonText = "Edit Name and Description";
    FormTag blockDescriptionForm =
        form(csrfTag).withMethod(HttpVerbs.POST).withAction(blockUpdateAction);
    blockDescriptionForm
        .withId("block-edit-form")
        .with(
            div(
                    h1("The following fields will only be visible to administrators")
                        .withClasses("text-base", "mb-2"),
                    FieldWithLabel.input()
                        .setId("block-name-input")
                        .setFieldName("name")
                        .setLabelText("Screen name")
                        .setValue(blockForm.getName())
                        .getInputTag(),
                    FieldWithLabel.textArea()
                        .setId("block-description-textarea")
                        .setFieldName("description")
                        .setLabelText("Screen description")
                        .setValue(blockForm.getDescription())
                        .getTextareaTag())
                .withClasses(Styles.MX_4),
            submitButton("Save")
                .withId("update-block-button")
                .withClasses(
                    Styles.MX_4,
                    Styles.MY_1,
                    Styles.INLINE,
                    Styles.OPACITY_100,
                    StyleUtils.disabled(Styles.OPACITY_50))
                .isDisabled());
    return Modal.builder("block-description-modal", blockDescriptionForm)
        .setModalTitle(modalTitle)
        .setTriggerButtonText(modalButtonText)
        .setWidth(Modal.Width.THIRD)
        .build();
  }

  private boolean hasNoRepeatedBlocks(ProgramDefinition programDefinition, long blockId) {
    return programDefinition.getBlockDefinitionsForEnumerator(blockId).isEmpty();
  }
}
