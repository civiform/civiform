package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
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
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.IntStream;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils;
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

/**
 * Renders a page for an admin to edit the configuration for a single block of a program.
 *
 * <p>Contains elements to:
 *
 * <ul>
 *   <li>Delete the block
 *   <li>Edit the name and description
 *   <li>View, add, delete and reorder questions
 *   <li>View and navigate to the visibility criteria
 * </ul>
 */
public final class ProgramBlockEditView extends ProgramBlockView {

  private final AdminLayout layout;
  private final boolean featureFlagOptionalQuestions;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  public static final String MOVE_QUESTION_POSITION_FIELD = "position";
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
      Optional<ToastMessage> message,
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
      Optional<ToastMessage> message,
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
            .addMainContent(
                div()
                    .withClasses(
                        "flex",
                        "flex-grow",
                        "flex-col",
                        "px-2",
                        StyleUtils.responsive2XLarge("px-16"))
                    .with(
                        addFormEndpoints(csrfTag, programDefinition.id(), blockId),
                        renderProgramInfo(programDefinition),
                        div()
                            .withClasses("flex", "flex-grow", "-mx-2")
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
                                    blockDescriptionEditModal.getButton()))),
                questionBankPanel(
                    questions,
                    programDefinition,
                    blockDefinition,
                    csrfTag,
                    QuestionBank.shouldShowQuestionBank(request)))
            .addModals(blockDescriptionEditModal);

    // Add toast messages
    request
        .flash()
        .get("error")
        .map(ToastMessage::error)
        .map(m -> m.setDuration(-1))
        .ifPresent(htmlBundle::addToastMessages);
    message.ifPresent(htmlBundle::addToastMessages);

    return layout.render(htmlBundle);
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

    return div(createBlockForm, createRepeatedBlockForm, deleteBlockForm).withClasses("hidden");
  }

  private DivTag blockOrderPanel(Request request, ProgramDefinition program, long focusedBlockId) {
    DivTag ret = div().withClasses("shadow-lg", "pt-6", "w-2/12", "border-r", "border-gray-200");
    ret.with(
        renderBlockList(
            request, program, program.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));

    ret.with(
        ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-4")
            .withType("submit")
            .withId("add-block-button")
            .withForm(CREATE_BLOCK_FORM_ID));
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
      String selectedClasses = blockDefinition.id() == focusedBlockId ? "bg-gray-100" : "";
      DivTag blockTag =
          div()
              .withClasses(
                  "flex",
                  "flex-row",
                  "gap-2",
                  "py-2",
                  "px-4",
                  "border",
                  "border-white",
                  StyleUtils.hover("border-gray-300"),
                  selectedClasses)
              .with(
                  a().withClasses("flex-grow", "overflow-hidden")
                      .withHref(editBlockLink)
                      .with(p(blockName), p(questionCountText).withClasses("text-sm")))
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
        blockDefinition.id() == blockDefinitions.get(0).id() ? "invisible" : "";
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
            ? "invisible"
            : "";
    DivTag moveDown =
        div()
            .withClasses("transform", "rotate-180", moveDownInvisible)
            .with(
                form()
                    .withAction(moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.DOWN.name()))
                    .with(submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    DivTag moveButtons =
        div().withClasses("flex", "flex-col", "self-center").with(moveUp, moveDown);
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
            .with(div(blockForm.getName()).withClasses("text-xl", "font-bold", "py-2"))
            .with(div(blockForm.getDescription()).withClasses("text-lg", "max-w-prose"))
            .withClasses("my-4");

    DivTag visibilityPredicateDisplay =
        renderVisibilityPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.visibilityPredicate(),
            blockDefinition.name(),
            allQuestions);

    DivTag eligibilityPredicateDisplay =
        renderEligibilityPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.eligibilityDefinition(),
            blockDefinition.name(),
            allQuestions);

    // Add buttons to change the block.
    DivTag buttons = div().withClasses("flex", "flex-row", "gap-4");
    buttons.with(blockDescriptionModalButton);
    if (blockDefinitionIsEnumerator) {
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

    DivTag programQuestions = div();
    IntStream.range(0, blockQuestions.size())
        .forEach(
            index -> {
              var question = blockQuestions.get(index);
              programQuestions.with(
                  renderQuestion(
                      csrfTag,
                      program.id(),
                      blockDefinition.id(),
                      question.getQuestionDefinition(),
                      canDelete,
                      question.optional(),
                      index,
                      blockQuestions.size()));
            });

    ButtonTag addQuestion =
        makeSvgTextButton("Add a question", Icons.ADD)
            .withClasses(
                AdminStyles.PRIMARY_BUTTON_STYLES,
                ReferenceClasses.OPEN_QUESTION_BANK_BUTTON,
                "my-4");

    return div()
        .withClasses("w-7/12", "py-6", "px-4")
        .with(
            blockInfoDisplay,
            buttons,
            visibilityPredicateDisplay,
            eligibilityPredicateDisplay,
            programQuestions,
            addQuestion);
  }

  private DivTag renderVisibilityPredicate(
      long programId,
      long blockId,
      Optional<PredicateDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions) {
    String currentBlockStatus =
        predicate.isEmpty()
            ? "This screen is always shown."
            : predicate.get().toDisplayString(blockName, questions);

    ButtonTag editScreenButton =
        ViewUtils.makeSvgTextButton("Edit visibility condition", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-2")
            .withId(ReferenceClasses.EDIT_PREDICATE_BUTTON);
    return div()
        .withClasses("my-4")
        .with(div("Visibility condition").withClasses("text-lg", "font-bold", "py-2"))
        .with(div(currentBlockStatus).withClasses("text-lg", "max-w-prose"))
        .with(
            asRedirectElement(
                editScreenButton,
                routes.AdminProgramBlockPredicatesController.edit(programId, blockId).url()));
  }

  private DivTag renderEligibilityPredicate(
      long programId,
      long blockId,
      Optional<EligibilityDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions) {
    String currentBlockStatus =
        predicate.isEmpty()
            ? "You can add eligibility conditions to help you screen out applicants who do not"
                + " meet the minimum requirements for a program early in the application"
                + " process."
            : predicate.get().predicate().toDisplayString(blockName, questions);

    ButtonTag editScreenButton =
        ViewUtils.makeSvgTextButton("Edit eligibility condition", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-2")
            .withId(ReferenceClasses.EDIT_PREDICATE_BUTTON);
    return div()
        .withClasses("my-4")
        .with(div("Eligibility condition").withClasses("text-lg", "font-bold", "py-2"))
        .with(div(currentBlockStatus).withClasses("text-lg", "max-w-prose"))
        .with(
            asRedirectElement(
                editScreenButton,
                routes.AdminProgramBlockPredicatesController.editEligibility(programId, blockId)
                    .url()));
  }

  private DivTag renderQuestion(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean canRemove,
      boolean isOptional,
      int questionIndex,
      int questionsCount) {
    DivTag ret =
        div()
            .withClasses(
                ReferenceClasses.PROGRAM_QUESTION,
                "my-2",
                "border",
                "border-gray-200",
                "px-4",
                "py-2",
                "flex",
                "gap-4",
                "items-center",
                StyleUtils.hover("text-gray-800", "bg-gray-100"));

    SvgTag icon =
        Icons.questionTypeSvg(questionDefinition.getQuestionType())
            .withClasses("shrink-0", "h-12", "w-6");
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();
    DivTag content =
        div()
            .withClass("flex-grow")
            .with(
                p(questionDefinition.getQuestionText().getDefault()),
                p(questionHelpText).withClasses("mt-1", "text-sm"),
                p(String.format("Admin ID: %s", questionDefinition.getName()))
                    .withClasses("mt-1", "text-sm"));

    Optional<FormTag> maybeOptionalToggle =
        optionalToggle(
            csrfTag, programDefinitionId, blockDefinitionId, questionDefinition, isOptional);

    ret.with(icon, content);
    if (maybeOptionalToggle.isPresent()) {
      ret.with(maybeOptionalToggle.get());
    }
    ret.with(
        this.createMoveQuestionButtonsSection(
            csrfTag,
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            questionIndex,
            questionsCount));
    return ret.with(
        deleteQuestionForm(
            csrfTag, programDefinitionId, blockDefinitionId, questionDefinition, canRemove));
  }

  private DivTag createMoveQuestionButtonsSection(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      int questionIndex,
      int questionsCount) {
    FormTag moveUp =
        this.createMoveQuestionButton(
            csrfTag,
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            Math.max(0, questionIndex - 1),
            Icons.ARROW_UPWARD,
            /* label= */ "move up",
            /* isInvisible= */ questionIndex == 0);
    FormTag moveDown =
        this.createMoveQuestionButton(
            csrfTag,
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            Math.min(questionsCount - 1, questionIndex + 1),
            Icons.ARROW_DOWNWARD,
            /* label= */ "move down",
            /* isInvisible= */ questionIndex == questionsCount - 1);
    return div().with(moveUp, moveDown);
  }

  private FormTag createMoveQuestionButton(
      InputTag csrfTag,
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
      InputTag csrfTag,
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
      InputTag csrfTag,
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

  private Modal blockDescriptionModal(
      InputTag csrfTag, BlockForm blockForm, String blockUpdateAction) {
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
}
