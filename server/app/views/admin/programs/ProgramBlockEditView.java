package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
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
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.QuestionBank;
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
  public ProgramBlockEditView(AdminLayout layout, Config config) {
    this.layout = checkNotNull(layout);
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
    Tag csrfTag = makeCsrfTokenInputTag(request);
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

  private Tag addFormEndpoints(Tag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    ContainerTag createBlockForm =
        form(csrfTag)
            .withId(CREATE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .attr("action", blockCreateAction);

    ContainerTag createRepeatedBlockForm =
        form(csrfTag)
            .withId(CREATE_REPEATED_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .attr("action", blockCreateAction)
            .with(
                FieldWithLabel.number()
                    .setFieldName(ENUMERATOR_ID_FORM_FIELD)
                    .setScreenReaderText(ENUMERATOR_ID_FORM_FIELD)
                    .setValue(OptionalLong.of(blockId))
                    .getContainer());

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    ContainerTag deleteBlockForm =
        form(csrfTag)
            .withId(DELETE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .attr("action", blockDeleteAction);

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm)
        .withClasses(Styles.HIDDEN);
  }

  private ContainerTag blockOrderPanel(
      Request request, ProgramDefinition program, long focusedBlockId) {
    ContainerTag ret =
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
            .attr(Attr.FORM, CREATE_BLOCK_FORM_ID)
            .withClasses(Styles.M_4));
    return ret;
  }

  private ContainerTag renderBlockList(
      Request request,
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

      ContainerTag moveButtons =
          blockMoveButtons(request, programDefinition.id(), blockDefinitions, blockDefinition);
      String selectedClasses = blockDefinition.id() == focusedBlockId ? Styles.BG_GRAY_100 : "";
      ContainerTag blockTag =
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

  private ContainerTag blockMoveButtons(
      Request request,
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {
    String moveUpFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? Styles.INVISIBLE : "";
    Tag moveUp =
        div()
            .withClass(moveUpInvisible)
            .with(
                form()
                    .attr("action", moveUpFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().attr("name", "direction").attr("value", Direction.UP.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));

    String moveDownFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? Styles.INVISIBLE
            : "";
    Tag moveDown =
        div()
            .withClasses(Styles.TRANSFORM, Styles.ROTATE_180, moveDownInvisible)
            .with(
                form()
                    .attr("action", moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().attr("name", "direction").attr("value", Direction.DOWN.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    ContainerTag moveButtons =
        div().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.SELF_CENTER).with(moveUp, moveDown);
    return moveButtons;
  }

  private ContainerTag blockEditPanel(
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean blockDefinitionIsEnumerator,
      Tag csrfTag,
      Tag blockDescriptionModalButton) {
    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canDelete =
        !blockDefinitionIsEnumerator || hasNoRepeatedBlocks(program, blockDefinition.id());

    ContainerTag blockInfoDisplay =
        div()
            .with(
                div(blockForm.getName()).withClasses(Styles.TEXT_XL, Styles.FONT_BOLD, Styles.PY_2))
            .with(div(blockForm.getDescription()).withClasses(Styles.TEXT_LG, Styles.MAX_W_PROSE))
            .withClasses(Styles.M_4);

    ContainerTag predicateDisplay =
        renderPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.visibilityPredicate(),
            blockDefinition.name(),
            allQuestions);

    // Add buttons to change the block.
    ContainerTag buttons =
        div().withClasses(Styles.MX_4, Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_4);
    buttons.with(blockDescriptionModalButton);
    if (blockDefinitionIsEnumerator) {
      buttons.with(
          submitButton("Create Repeated Screen")
              .withId("create-repeated-block-button")
              .attr(Attr.FORM, CREATE_REPEATED_BLOCK_FORM_ID));
    }
    // TODO: Maybe add alpha variants to button color on hover over so we do not have
    //  to hard-code what the color will be when button is in hover state?
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass(Styles.FLEX_GROW));
      buttons.with(
          submitButton("Delete Screen")
              .withId("delete-block-button")
              .attr(Attr.FORM, DELETE_BLOCK_FORM_ID)
              .condAttr(!canDelete, Attr.DISABLED, "")
              .condAttr(
                  !canDelete,
                  Attr.TITLE,
                  "A screen can only be deleted when it has no repeated screens.")
              .withClasses(
                  Styles.MX_4,
                  Styles.MY_1,
                  Styles.BG_RED_500,
                  StyleUtils.hover(Styles.BG_RED_700),
                  Styles.INLINE,
                  StyleUtils.disabled(Styles.OPACITY_50)));
    }

    ContainerTag programQuestions = div();
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

  private ContainerTag renderPredicate(
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

  private ContainerTag renderQuestion(
      Tag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove,
      boolean isOptional) {
    ContainerTag ret =
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

    ContainerTag icon =
        Icons.questionTypeSvg(questionDefinition.getQuestionType(), 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    ContainerTag content =
        div()
            .withClass(Styles.FLEX_GROW)
            .with(p(questionDefinition.getName()))
            .with(p(questionDefinition.getDescription()).withClasses(Styles.MT_1, Styles.TEXT_SM));

    Optional<Tag> maybeOptionalToggle =
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

  private Optional<Tag> optionalToggle(
      Tag csrfTag,
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
    ContainerTag optionalButton =
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
            .attr("type", "submit")
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
            .attr("action", toggleOptionalAction)
            .with(input().isHidden().attr("name", "optional").attr("value", isOptional ? "false" : "true"))
            .with(optionalButton));
  }

  private Tag deleteQuestionForm(
      Tag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove) {
    Tag removeButton =
        TagCreator.button(text("DELETE"))
            .attr("type", "submit")
            .withId("block-question-" + questionDefinition.getId())
            .attr("name", "questionDefinitionId")
            .attr("value", String.valueOf(questionDefinition.getId()))
            .condAttr(!canRemove, Attr.DISABLED, "")
            .condAttr(
                !canRemove,
                Attr.TITLE,
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
        .attr("action", deleteQuestionAction)
        .with(removeButton);
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

  private Modal blockDescriptionModal(Tag csrfTag, BlockForm blockForm, String blockUpdateAction) {
    String modalTitle = "Screen Name and Description";
    String modalButtonText = "Edit Name and Description";
    ContainerTag blockDescriptionForm =
        form(csrfTag).withMethod(HttpVerbs.POST).withAction(blockUpdateAction);
    blockDescriptionForm
        .withId("block-edit-form")
        .with(
            div(
                    FieldWithLabel.input()
                        .setId("block-name-input")
                        .setFieldName("name")
                        .setLabelText("Screen name")
                        .setValue(blockForm.getName())
                        .getContainer(),
                    FieldWithLabel.textArea()
                        .setId("block-description-textarea")
                        .setFieldName("description")
                        .setLabelText("Screen description")
                        .setValue(blockForm.getDescription())
                        .getContainer())
                .withClasses(Styles.MX_4),
            submitButton("Save")
                .withId("update-block-button")
                .withClasses(
                    Styles.MX_4,
                    Styles.MY_1,
                    Styles.INLINE,
                    Styles.OPACITY_100,
                    StyleUtils.disabled(Styles.OPACITY_50))
                .attr("disabled", ""));
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
