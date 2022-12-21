package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import controllers.admin.routes;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.QuestionBank;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Renders a page for an admin to edit the configuration for program, including a single block of a
 * program. A block is a synonym for a Screen. The ProgramBlockEditView is very similar to the
 * ProgramBlockReadOnlyView, but specifically adds all UI functionality that is needed for editing.
 */
public final class DraftProgramBlockEditView extends ActiveProgramBlockReadOnlyView {

  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";

  private final boolean featureFlagOptionalQuestions;

  private Optional<Modal> blockDescriptionEditModal = Optional.empty();
  private InputTag csrfTag;

  @Inject
  public DraftProgramBlockEditView(AdminLayoutFactory layoutFactory, Config config) {
    super(layoutFactory, config);
    this.featureFlagOptionalQuestions = checkNotNull(config).hasPath("cf.optional_questions");
  }

  @Override
  public Content render(
      Request request,
      ProgramDefinition programDefinition,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      Optional<ToastMessage> message,
      ImmutableList<QuestionDefinition> questions) {

    if(blockDescriptionEditModal.isEmpty()) {
      String blockUpdateAction = controllers.admin.routes.AdminProgramBlocksController
        .update( programDefinition.id(), blockDefinition.id()) .url();
      blockDescriptionEditModal = Optional.of(blockDescriptionModal(blockForm, blockUpdateAction));
    }

    csrfTag = makeCsrfTokenInputTag(request);
    return super.render(request, programDefinition, blockForm, blockDefinition, message, questions);
  }

  @Override
  protected HtmlBundle createHtmlBundle(
      Request request,
      ProgramDefinition programDefinition,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> questions) {

    HtmlBundle htmlBundle = super.createHtmlBundle(request, programDefinition,
      blockForm, blockDefinition, questions);
    htmlBundle.addMainContent(
        questionBankPanel(
          questions,
          programDefinition,
          blockDefinition,
          QuestionBank.shouldShowQuestionBank(request)))
      .addMainContent(
        addFormEndpoints(programDefinition.id(), blockDefinition.id()));
    blockDescriptionEditModal.ifPresent(htmlBundle::addModals);

    return htmlBundle;
  }

  @Override
  protected DivTag blockOrderPanel(
      Request request, ProgramDefinition program, long focusedBlockId) {
    return super.blockOrderPanel(request, program, focusedBlockId)
        .with(
            ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
                .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-4")
                .withType("submit")
                .withId("add-block-button")
                .withForm(CREATE_BLOCK_FORM_ID));
  }

  @Override
  protected DivTag blockTag(
      ImmutableList<BlockDefinition> blockDefinitions,
      int blockIndex,
      ProgramDefinition programDefinition,
      long focusedBlockId) {

    DivTag blockTag =
        super.blockTag(blockDefinitions, blockIndex, programDefinition, focusedBlockId);

    BlockDefinition blockDefinition = blockDefinitions.get(blockIndex);
    DivTag moveButtons =
        blockMoveButtons(programDefinition.id(), blockDefinitions, blockDefinition);
    return blockTag.with(moveButtons);
  }

  @Override
  protected ArrayList<DomContent> prepareContentForBlockPanel(
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      BlockForm blockForm,
      ImmutableList<QuestionDefinition> allQuestions) {
    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(
                program.id(), blockDefinition.id())
            .url();

    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canDelete =
        !blockDefinition.isEnumerator() || hasNoRepeatedBlocks(program, blockDefinition.id());

    // Add buttons to change the block.
    DivTag buttons = div().withClasses("flex", "flex-row", "gap-4");
    blockDescriptionEditModal.ifPresent(modal -> buttons.with(modal.getButton()));
    if (blockDefinition.isEnumerator()) {
      buttons.with(
          button("Create repeated screen")
              .withType("submit")
              .withId("create-repeated-block-button")
              .withForm(CREATE_REPEATED_BLOCK_FORM_ID)
              .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES));
    }
    // TODO: Maybe add alpha variants to button color on hover over so we do not have
    //  to hard-code what the color will be when button is in hover state?
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass("flex-grow"));
      buttons.with(
          ViewUtils.makeSvgTextButton("Delete screen", Icons.DELETE)
              .withType("submit")
              .withId("delete-block-button")
              .withForm(DELETE_BLOCK_FORM_ID)
              .withCondDisabled(!canDelete)
              .withCondTitle(
                  !canDelete, "A screen can only be deleted when it has no repeated screens.")
              .withClasses(
                  AdminStyles.SECONDARY_BUTTON_STYLES,
                  "mx-4",
                  "my-1",
                  StyleUtils.disabled("opacity-50")));
    }

    ButtonTag addQuestion =
        makeSvgTextButton("Add a question", Icons.ADD)
            .withClasses(
                AdminStyles.PRIMARY_BUTTON_STYLES,
                ReferenceClasses.OPEN_QUESTION_BANK_BUTTON,
                "my-4");

    ArrayList<DomContent> content =
        super.prepareContentForBlockPanel(program, blockDefinition, blockForm, allQuestions);

    content.add(1, buttons);
    content.add(addQuestion);

    return content;
  }

  @Override
  protected DivTag renderPredicate(
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> questions) {
    DivTag ret = super.renderPredicate(programDefinition, blockDefinition, questions);

    ButtonTag editScreenButton =
        ViewUtils.makeSvgTextButton("Edit visibility condition", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-2")
            .withId(ReferenceClasses.EDIT_VISIBILITY_PREDICATE_BUTTON);

    return ret.with(
        asRedirectElement(
            editScreenButton,
            routes.AdminProgramBlockPredicatesController.edit(
                    programDefinition.id(), blockDefinition.id())
                .url()));
  }

  @Override
  protected DivTag renderQuestion(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition, int questionIndex) {

    ImmutableList<ProgramQuestionDefinition> blockQuestions =
        blockDefinition.programQuestionDefinitions();
    ProgramQuestionDefinition question = blockQuestions.get(questionIndex);
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    int questionsCount = blockQuestions.size();
    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canRemove =
        !blockDefinition.isEnumerator()
            || hasNoRepeatedBlocks(programDefinition, blockDefinition.id());

    DivTag ret = super.renderQuestion(programDefinition, blockDefinition, questionIndex);

    Optional<FormTag> maybeOptionalToggle =
        optionalToggle(
            programDefinition.id(), blockDefinition.id(), questionDefinition, question.optional());
    if (maybeOptionalToggle.isPresent()) {
      ret.with(maybeOptionalToggle.get());
    }

    ret.with(
        this.moveQuestionButtonsSection(
            programDefinition.id(),
            blockDefinition.id(),
            questionDefinition,
            questionIndex,
            questionsCount));
    return ret.with(
        deleteQuestionForm(
            programDefinition.id(), blockDefinition.id(), questionDefinition, canRemove));
  }

  /*
   * Creates an invisible form used for html request building.
   * TODO: There may be better ways to generate the request than using an invisible form?
   */
  private DivTag addFormEndpoints(long programId, long blockId) {
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

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm).withClasses("hidden");
  }

  private DivTag blockMoveButtons(
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {

    String moveUpFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? "invisible" : "";
    DivTag moveUp =
        div()
            .withClass(moveUpInvisible)
            .with(
                form()
                    .withAction(moveUpFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(csrfTag)
                    .with(input().isHidden().withName("direction").withValue(Direction.UP.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));

    String moveDownFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? "invisible"
            : "";
    DivTag moveDown =
        div()
            .withClasses("transform", "rotate-180", moveDownInvisible)
            .with(
                form()
                    .withAction(moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(csrfTag)
                    .with(input().isHidden().withName("direction").withValue(Direction.DOWN.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    DivTag moveButtons =
        div().withClasses("flex", "flex-col", "self-center").with(moveUp, moveDown);
    return moveButtons;
  }

  private DivTag moveQuestionButtonsSection(
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      int questionIndex,
      int questionsCount) {

    FormTag moveUp =
        this.moveQuestionButton(
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            Math.max(0, questionIndex - 1),
            Icons.ARROW_UPWARD,
            /* label= */ "move up",
            /* isInvisible= */ questionIndex == 0);
    FormTag moveDown =
        this.moveQuestionButton(
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            Math.min(questionsCount - 1, questionIndex + 1),
            Icons.ARROW_DOWNWARD,
            /* label= */ "move down",
            /* isInvisible= */ questionIndex == questionsCount - 1);
    return div().with(moveUp, moveDown);
  }

  private FormTag moveQuestionButton(
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      int newIndex,
      Icons icon,
      String label,
      boolean isInvisible) {
    ButtonTag button =
        submitButton("")
            .with(Icons.svg(icon).withClasses("w-6", "h-6"))
            .withClasses(AdminStyles.MOVE_BLOCK_BUTTON)
            .attr("aria-label", label);
    String moveAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.move(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
    return form(csrfTag)
        .withClasses("inline-block", "mx-1", isInvisible ? "invisible" : "")
        .withMethod(HttpVerbs.POST)
        .withAction(moveAction)
        .with(
            input()
                .isHidden()
                .withName(MOVE_QUESTION_POSITION_FIELD)
                .withValue(String.valueOf(newIndex)))
        .with(button);
  }

  private Optional<FormTag> optionalToggle(
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
                "flex",
                "gap-2",
                "items-center",
                isOptional ? "text-black" : "text-gray-400",
                "font-medium",
                "bg-transparent",
                "rounded-full",
                StyleUtils.hover("bg-gray-400", "text-gray-300"))
            .withType("submit")
            .with(p("optional").withClasses("hover-group:text-white"))
            .with(
                div()
                    .withClasses("relative")
                    .with(
                        div()
                            .withClasses(
                                isOptional ? "bg-blue-600" : "bg-gray-600",
                                "w-14",
                                "h-8",
                                "rounded-full"))
                    .with(
                        div()
                            .withClasses(
                                "absolute",
                                "bg-white",
                                isOptional ? "right-1" : "left-1",
                                "top-1",
                                "w-6",
                                "h-6",
                                "rounded-full")));
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
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove) {
    ButtonTag removeButton =
        ViewUtils.makeSvgTextButton("Delete", Icons.DELETE)
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
                ReferenceClasses.REMOVE_QUESTION_BUTTON,
                AdminStyles.SECONDARY_BUTTON_STYLES,
                canRemove ? "" : "opacity-50");
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

  private DivTag questionBankPanel(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      QuestionBank.Visibility questionBankVisibility) {
    String addQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                program.id(), blockDefinition.id())
            .url();

    String redirectUrl =
        QuestionBank.addShowQuestionBankParam(
            controllers.admin.routes.AdminProgramBlocksController.edit(
                    program.id(), blockDefinition.id())
                .url());
    QuestionBank qb =
        new QuestionBank(
            QuestionBank.QuestionBankParams.builder()
                .setQuestionAction(addQuestionAction)
                .setCsrfTag(csrfTag)
                .setQuestions(questionDefinitions)
                .setProgram(program)
                .setBlockDefinition(blockDefinition)
                .setQuestionCreateRedirectUrl(redirectUrl)
                .build());
    return qb.getContainer(questionBankVisibility);
  }

  /** Modal dialog shown when the admin edits the screen name and description * */
  private Modal blockDescriptionModal(BlockForm blockForm, String blockUpdateAction) {
    String modalTitle = "Screen name and description";
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
                .withClasses("mx-4"),
            submitButton("Save")
                .withId("update-block-button")
                .withClasses(
                    "mx-4", "my-1", "inline", "opacity-100", StyleUtils.disabled("opacity-50"))
                .isDisabled());
    ButtonTag editScreenButton =
        ViewUtils.makeSvgTextButton("Edit screen name and description", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES);
    return Modal.builder("block-description-modal", blockDescriptionForm)
        .setModalTitle(modalTitle)
        .setTriggerButtonContent(editScreenButton)
        .setWidth(Modal.Width.THIRD)
        .build();
  }

  private boolean hasNoRepeatedBlocks(ProgramDefinition programDefinition, long blockId) {
    return programDefinition.getBlockDefinitionsForEnumerator(blockId).isEmpty();
  }

  @Override
  protected String getBlockSelectionUrl(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition) {
    return controllers.admin.routes.AdminProgramBlocksController.edit(
            programDefinition.id(), blockDefinition.id())
        .url();
  }

  @Override
  protected String getEditButtonText() {
    return "Edit program details";
  }

  @Override
  protected String getEditButtonUrl(ProgramDefinition programDefinition) {
    return routes.AdminProgramController.edit(programDefinition.id()).url();
  }

  @Override
  protected BadgeStatus getBadgeStatus() {
    return BadgeStatus.DRAFT;
  }
}
