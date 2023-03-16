package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.b;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.join;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.typesafe.config.Config;
import controllers.admin.routes;
import featureflags.FeatureFlags;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.ArrayList;
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
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
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
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Renders a page for an admin to view or edit the configuration for a single block of an active or
 * draft program.
 *
 * <p>For editing drafts this contains elements to:
 *
 * <ul>
 *   <li>Delete the block
 *   <li>Edit the name and description
 *   <li>View, add, delete and reorder questions
 *   <li>View and navigate to the visibility criteria
 * </ul>
 *
 * For viewing an active program, it contains the same elements, but without UI elements that can be
 * used for editing. TODO(#4019) Rename this to ProgramBlockView
 */
public final class ProgramBlockEditView extends ProgramBlockBaseView {

  private final AdminLayout layout;
  private final FeatureFlags featureFlags;
  private final boolean featureFlagOptionalQuestions;
  private final ProgramDisplayType programDisplayType;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  public static final String MOVE_QUESTION_POSITION_FIELD = "position";
  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";
  private static final int BASE_INDENTATION_SIZE = 4;
  private static final int INDENTATION_FACTOR_INCREASE_ON_LEVEL = 2;

  @Inject
  public ProgramBlockEditView(
      @Assisted ProgramDisplayType programViewType,
      AdminLayoutFactory layoutFactory,
      Config config,
      FeatureFlags featureFlags) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.featureFlags = checkNotNull(featureFlags);
    this.featureFlagOptionalQuestions = checkNotNull(config).hasPath("cf.optional_questions");
    this.programDisplayType = programViewType;
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

    String title =
        viewAllowsEditingProgram()
            ? String.format("Edit %s", blockDefinition.name())
            : String.format("View %s", blockDefinition.name());
    Long programId = programDefinition.id();

    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(programId, blockId).url();
    Modal blockDescriptionEditModal = blockDescriptionModal(csrfTag, blockForm, blockUpdateAction);

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.destroy(programId, blockId).url();
    Modal blockDeleteScreenModal = blockDeleteModal(csrfTag, blockDeleteAction, blockDefinition);

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
                        renderProgramInfo(programDefinition)
                            .with(renderEditButton(request, programDefinition)),
                        div()
                            .withClasses("flex", "flex-grow", "-mx-2")
                            .with(blockOrderPanel(request, programDefinition, blockId))
                            .with(
                                blockPanel(
                                    programDefinition,
                                    blockDefinition,
                                    blockForm,
                                    blockQuestions,
                                    questions,
                                    blockDefinition.isEnumerator(),
                                    csrfTag,
                                    blockDescriptionEditModal.getButton(),
                                    blockDeleteScreenModal.getButton(),
                                    featureFlags.isProgramEligibilityConditionsEnabled(request),
                                    featureFlags.isIntakeFormEnabled(request),
                                    request))));

    // Add top level UI that is only visible in the editable version.
    if (viewAllowsEditingProgram()) {
      htmlBundle
          .addMainContent(
              questionBankPanel(
                  questions,
                  programDefinition,
                  blockDefinition,
                  csrfTag,
                  QuestionBank.shouldShowQuestionBank(request)))
          .addMainContent(addFormEndpoints(csrfTag, programDefinition.id(), blockId))
          .addModals(blockDescriptionEditModal, blockDeleteScreenModal);
    }

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

    return div(createBlockForm, createRepeatedBlockForm).withClasses("hidden");
  }

  private DivTag blockOrderPanel(Request request, ProgramDefinition program, long focusedBlockId) {
    DivTag ret = div().withClasses("shadow-lg", "pt-6", "w-2/12", "border-r", "border-gray-200");
    ret.with(
        renderBlockList(
            request,
            program,
            program.getNonRepeatedBlockDefinitions(),
            focusedBlockId,
            /* level= */ 0));

    if (viewAllowsEditingProgram()) {
      ret.with(
          ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
              .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-4")
              .withType("submit")
              .withId("add-block-button")
              .withForm(CREATE_BLOCK_FORM_ID));
    }
    return ret;
  }

  private DivTag renderBlockList(
      Request request,
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level) {
    DivTag container = div();
    String genericBlockDivId = "block_list_item_";
    for (BlockDefinition blockDefinition : blockDefinitions) {

      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      String blockName = blockDefinition.name();
      // indentation value for enums and repeaters
      int listIndentationFactor =
          BASE_INDENTATION_SIZE + (level * INDENTATION_FACTOR_INCREASE_ON_LEVEL);
      String selectedClasses = blockDefinition.id() == focusedBlockId ? "bg-gray-100" : "";
      DivTag blockTag =
          div()
              .withClasses(
                  "flex",
                  "flex-row",
                  "gap-2",
                  "py-2",
                  "px-" + listIndentationFactor,
                  "border",
                  "border-white",
                  StyleUtils.hover("border-gray-300"),
                  selectedClasses);
      String switchBlockLink;
      if (viewAllowsEditingProgram()) {
        switchBlockLink =
            controllers.admin.routes.AdminProgramBlocksController.edit(
                    programDefinition.id(), blockDefinition.id())
                .url();
      } else {
        switchBlockLink =
            controllers.admin.routes.AdminProgramBlocksController.show(
                    programDefinition.id(), blockDefinition.id())
                .url();
      }
      blockTag
          .withId(genericBlockDivId + blockDefinition.id())
          .with(
              a().withClasses("flex-grow", "overflow-hidden")
                  .withHref(switchBlockLink)
                  .with(p(blockName), p(questionCountText).withClasses("text-sm")));
      if (viewAllowsEditingProgram()) {
        DivTag moveButtons =
            blockMoveButtons(request, programDefinition.id(), blockDefinitions, blockDefinition);
        blockTag.with(moveButtons);
      }
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
    return div().withClasses("flex", "flex-col", "self-center").with(moveUp, moveDown);
  }

  private DivTag blockPanel(
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean blockDefinitionIsEnumerator,
      InputTag csrfTag,
      ButtonTag blockDescriptionModalButton,
      ButtonTag blockDeleteModalButton,
      boolean isProgramEligibilityConditionsEnabled,
      boolean isIntakeFormFeatureEnabled,
      Request request) {
    // A block can only be deleted when it has no repeated blocks. Same is true for removing the
    // enumerator question from the block.
    final boolean canDelete =
        !blockDefinitionIsEnumerator || hasNoRepeatedBlocks(program, blockDefinition.id());

    DivTag blockInfoDisplay =
        div()
            .with(div(blockForm.getName()).withClasses("text-xl", "font-bold", "py-2"))
            .with(div(blockForm.getDescription()).withClasses("text-lg", "max-w-prose"))
            .withId("block-info-display-" + blockDefinition.id())
            .withClasses("my-4");

    DivTag visibilityPredicateDisplay =
        renderVisibilityPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.visibilityPredicate(),
            blockDefinition.name(),
            allQuestions);

    Optional<DivTag> maybeEligibilityPredicateDisplay = Optional.empty();
    if (isProgramEligibilityConditionsEnabled
        && !(isIntakeFormFeatureEnabled
            && program.programType().equals(ProgramType.COMMON_INTAKE_FORM))) {
      maybeEligibilityPredicateDisplay =
          Optional.of(
              renderEligibilityPredicate(
                  program,
                  blockDefinition.id(),
                  blockDefinition.eligibilityDefinition(),
                  blockDefinition.name(),
                  allQuestions,
                  request));
    }

    DivTag programQuestions = div();
    IntStream.range(0, blockQuestions.size())
        .forEach(
            index -> {
              var question = blockQuestions.get(index);
              programQuestions.with(
                  renderQuestion(
                      csrfTag,
                      program,
                      blockDefinition,
                      question.getQuestionDefinition(),
                      canDelete,
                      question.optional(),
                      question.addressCorrectionEnabled(),
                      index,
                      blockQuestions.size(),
                      request));
            });

    DivTag div = div().withClasses("w-7/12", "py-6", "px-4");

    // UI elements for editing are only needed when we view a draft
    if (viewAllowsEditingProgram()) {
      DivTag buttons =
          blockPanelButtons(
              program,
              blockDefinitionIsEnumerator,
              blockDescriptionModalButton,
              blockDeleteModalButton,
              canDelete);
      ButtonTag addQuestion =
          makeSvgTextButton("Add a question", Icons.ADD)
              .withClasses(
                  AdminStyles.PRIMARY_BUTTON_STYLES,
                  ReferenceClasses.OPEN_QUESTION_BANK_BUTTON,
                  "my-4");

      div.with(blockInfoDisplay, buttons, visibilityPredicateDisplay);
      maybeEligibilityPredicateDisplay.ifPresent(div::with);
      return div.with(programQuestions, addQuestion);
    } else {
      div.with(blockInfoDisplay, visibilityPredicateDisplay);
      maybeEligibilityPredicateDisplay.ifPresent(div::with);
      return div.with(programQuestions);
    }
  }

  private DivTag blockPanelButtons(
      ProgramDefinition program,
      boolean blockDefinitionIsEnumerator,
      ButtonTag blockDescriptionModalButton,
      ButtonTag blockDeleteModalButton,
      Boolean canDelete) {

    // Add buttons to change the block.
    DivTag buttons = div().withClasses("flex", "flex-row", "gap-4");

    // Buttons are only needed when the view is used for editing
    buttons.with(blockDescriptionModalButton);
    buttons.condWith(
        blockDefinitionIsEnumerator,
        button("Create repeated screen")
            .withType("submit")
            .withId("create-repeated-block-button")
            .withForm(CREATE_REPEATED_BLOCK_FORM_ID)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES));

    // TODO: Maybe add alpha variants to button color on hover over so we do not have
    //  to hard-code what the color will be when button is in hover state?

    // Only add the delete button if there is more than one screen in the program
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass("flex-grow"));
      if (canDelete) {
        buttons.with(blockDeleteModalButton);
      } else {
        buttons.with(
            blockDeleteModalButton
                .withCondDisabled(!canDelete)
                .withCondTitle(
                    !canDelete, "A screen can only be deleted when it has no repeated screens."));
      }
    }
    return buttons;
  }

  private DivTag renderVisibilityPredicate(
      long programId,
      long blockId,
      Optional<PredicateDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions) {
    DivTag currentBlockStatus =
        predicate.isEmpty()
            ? div("This screen is always shown.")
            : renderExistingPredicate(blockName, predicate.get(), questions);

    DivTag div =
        div()
            .withClasses("my-4")
            .with(div("Visibility condition").withClasses("text-lg", "font-bold", "py-2"))
            .with(currentBlockStatus.withClasses("text-lg", "max-w-prose"));

    if (viewAllowsEditingProgram()) {
      ButtonTag editScreenButton =
          ViewUtils.makeSvgTextButton("Edit visibility condition", Icons.EDIT)
              .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-2")
              .withId(ReferenceClasses.EDIT_VISIBILITY_PREDICATE_BUTTON);
      div.with(
          asRedirectElement(
              editScreenButton,
              routes.AdminProgramBlockPredicatesController.editVisibility(programId, blockId)
                  .url()));
    }
    return div;
  }

  private DivTag renderEligibilityPredicate(
      ProgramDefinition program,
      long blockId,
      Optional<EligibilityDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions,
      Request request) {
    DivTag currentBlockStatus =
        predicate.isEmpty()
            ? renderEmptyEligibilityPredicate(program, request)
            : renderExistingPredicate(blockName, predicate.get().predicate(), questions);
    DivTag div =
        div()
            .withClasses("my-4")
            .with(div("Eligibility condition").withClasses("text-lg", "font-bold", "py-2"))
            .with(currentBlockStatus.withClasses("text-lg", "max-w-prose"));

    if (viewAllowsEditingProgram()) {
      ButtonTag editScreenButton =
          ViewUtils.makeSvgTextButton("Edit eligibility condition", Icons.EDIT)
              .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "m-2")
              .withId(ReferenceClasses.EDIT_ELIGIBILITY_PREDICATE_BUTTON);
      div.with(
          asRedirectElement(
              editScreenButton,
              routes.AdminProgramBlockPredicatesController.editEligibility(program.id(), blockId)
                  .url()));
    }
    return div;
  }

  private DivTag renderEmptyEligibilityPredicate(ProgramDefinition program, Request request) {
    DivTag emptyPredicateDiv;
    if (featureFlags.isNongatedEligibilityEnabled(request)) {
      ImmutableList.Builder<DomContent> emptyPredicateContentBuilder = ImmutableList.builder();
      if (program.eligibilityIsGating()) {
        emptyPredicateContentBuilder.add(
            text(
                "You can add eligibility conditions to determine if an applicant qualifies for the"
                    + " program. Applicants who do not meet the minimum requirements will be"
                    + " blocked from submitting an application."));
      } else {
        emptyPredicateContentBuilder.add(
            text(
                "You can add eligibility conditions to determine if an applicant qualifies for the"
                    + " program. Applicants can submit an application even if they do not meet the"
                    + " minimum requirements."));
      }
      emptyPredicateContentBuilder
          .add(text(" You can change this in "))
          .add(
              a().withText("program settings.")
                  .withHref(routes.AdminProgramController.editProgramSettings(program.id()).url())
                  .withClasses(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT));
      emptyPredicateDiv = div().with(emptyPredicateContentBuilder.build());
    } else {
      emptyPredicateDiv =
          div(
              "You can add eligibility conditions to help screen applicants who do not"
                  + " meet the minimum requirements for a program early in the application"
                  + " process.");
    }
    return emptyPredicateDiv;
  }

  private DivTag renderQuestion(
      InputTag csrfTag,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      QuestionDefinition questionDefinition,
      boolean canRemove,
      boolean isOptional,
      boolean addressCorrectionEnabled,
      int questionIndex,
      int questionsCount,
      Request request) {
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
            csrfTag, programDefinition.id(), blockDefinition.id(), questionDefinition, isOptional);

    Optional<FormTag> maybeAddressCorrectionEnabledToggle =
        addressCorrectionEnabledToggle(
            request,
            csrfTag,
            programDefinition,
            blockDefinition,
            questionDefinition,
            addressCorrectionEnabled);

    ret.with(icon, content);
    // UI for editing is only added if we are viewing a draft.
    if (viewAllowsEditingProgram()) {
      maybeAddressCorrectionEnabledToggle.ifPresent(toggle -> ret.with(toggle));
      maybeOptionalToggle.ifPresent(ret::with);
      ret.with(
          this.createMoveQuestionButtonsSection(
              csrfTag,
              programDefinition.id(),
              blockDefinition.id(),
              questionDefinition,
              questionIndex,
              questionsCount));
      ret.with(
          deleteQuestionForm(
              csrfTag,
              programDefinition.id(),
              blockDefinition.id(),
              questionDefinition,
              canRemove));
    } else {
      // For each toggle, use a label instead in the read only view
      if (maybeAddressCorrectionEnabledToggle.isPresent()) {
        String label =
            addressCorrectionEnabled
                ? "Address correction: enabled"
                : "Address correction: disabled";
        ret.with(makeReadOnlyLabel(label));
      }
      if (maybeOptionalToggle.isPresent()) {
        String label = isOptional ? "optional question" : "required question";
        ret.with(makeReadOnlyLabel(label));
      }
    }
    return ret;
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

  // Creates label that can be used in the read only view, for example to replace a toggle.
  private DivTag makeReadOnlyLabel(String label) {
    DivTag ret =
        div()
            .withClasses(
                "flex",
                "gap-2",
                "items-center",
                "text-gray-400",
                "font-medium",
                "bg-transparent",
                "rounded-full")
            .with(p(label));
    return ret;
  }

  private Optional<FormTag> addressCorrectionEnabledToggle(
      Request request,
      InputTag csrfTag,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      QuestionDefinition questionDefinition,
      boolean addressCorrectionEnabled) {
    if (!questionDefinition.isAddress()) {
      return Optional.empty();
    }

    boolean questionIsUsedInPredicate =
        programDefinition.isQuestionUsedInPredicate(questionDefinition.getId());

    String toolTipText =
        "Enabling address correction will check the resident's address to ensure it is accurate.";
    if (!featureFlags.isEsriAddressCorrectionEnabled(request)) {
      toolTipText +=
          " To use this feature, you will need to have your IT manager configure the GIS service.";
    }
    if (questionIsUsedInPredicate) {
      toolTipText +=
          " Questions used in visibility or eligibility conditions must have address correction"
              + " enabled.";
    }

    boolean addressCorrectionEnabledQuestionAlreadyExists =
        blockDefinition.hasAddressCorrectionEnabledOnDifferentQuestion(questionDefinition.getId());
    if (addressCorrectionEnabledQuestionAlreadyExists) {
      toolTipText +=
          " This screen already contains a question with address correction enabled. This feature"
              + " can only be enabled once per screen.";
    }

    ButtonTag addressCorrectionButton =
        TagCreator.button()
            .withClasses(
                "flex",
                "gap-2",
                "items-center",
                addressCorrectionEnabled ? "text-black" : "text-gray-400",
                "font-medium",
                "bg-transparent",
                "rounded-full",
                StyleUtils.hover("bg-gray-400", "text-gray-300"))
            .withType("submit")
            .with(p("Address correction").withClasses("hover-group:text-white"))
            .with(
                div()
                    .withClasses("relative")
                    .with(
                        div()
                            .withClasses(
                                addressCorrectionEnabled ? "bg-blue-600" : "bg-gray-600",
                                "w-14",
                                "h-8",
                                "rounded-full"))
                    .with(
                        div()
                            .withClasses(
                                "absolute",
                                "bg-white",
                                addressCorrectionEnabled ? "right-1" : "left-1",
                                "top-1",
                                "w-6",
                                "h-6",
                                "rounded-full")))
            .with(div(ViewUtils.makeSvgToolTipRightAnchored(toolTipText, Icons.HELP)));
    String toggleAddressCorrectionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.setAddressCorrectionEnabled(
                programDefinition.id(), blockDefinition.id(), questionDefinition.getId())
            .url();
    return Optional.of(
        form(csrfTag)
            .withMethod(HttpVerbs.POST)
            .withCondOnsubmit(
                !featureFlags.isEsriAddressCorrectionEnabled(request) || questionIsUsedInPredicate,
                "return false;")
            .withCondOnsubmit(addressCorrectionEnabledQuestionAlreadyExists, "return false;")
            .withAction(toggleAddressCorrectionAction)
            .with(
                input()
                    .isHidden()
                    .withName("addressCorrectionEnabled")
                    .withValue(addressCorrectionEnabled ? "false" : "true"))
            .with(addressCorrectionButton));
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

  private Modal blockDeleteModal(
      InputTag csrfTag, String blockDeleteAction, BlockDefinition blockDefinition) {

    FormTag deleteBlockForm =
        form(csrfTag)
            .withId(DELETE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockDeleteAction);

    boolean hasQuestions = blockDefinition.getQuestionCount() > 0;
    boolean hasEligibilityCondition = !blockDefinition.eligibilityDefinition().isEmpty();
    boolean hasVisibilityCondition = !blockDefinition.visibilityPredicate().isEmpty();
    ArrayList<String> itemsInBlock = new ArrayList<String>();

    if (hasQuestions) {
      itemsInBlock.add("questions");
    }
    if (hasEligibilityCondition) {
      itemsInBlock.add("eligibility conditions");
    }
    if (hasVisibilityCondition) {
      itemsInBlock.add("visibility conditions");
    }

    // If there are no questions, eligibilty conditions, or visibility conditions on this screen,
    // just print "Are you sure you want to delete this screen?"
    if (itemsInBlock.size() == 0) {
      deleteBlockForm
          .withId("block-delete-form")
          .with(
              div(h1("Are you sure you want to delete this screen?")
                      .withClasses("text-base", "mb-2"))
                  .withClasses("mx-4"),
              submitButton("Delete")
                  .withId("delete-block-button")
                  .withClasses(
                      "mx-4", "my-1", "inline", "opacity-100", StyleUtils.disabled("opacity-50")));
    } else {
      // If there are questions, eligibility conditions, or visibility conditions on this screen,
      // print the appropriate message.
      String listItemsInBlock = "";
      if (itemsInBlock.size() == 1) {
        listItemsInBlock = itemsInBlock.get(0);
      } else if (itemsInBlock.size() == 2) {
        listItemsInBlock = itemsInBlock.get(0) + " and " + itemsInBlock.get(1);
      } else {
        // there will only ever be 1, 2, or 3 items
        listItemsInBlock =
            itemsInBlock.get(0) + ", " + itemsInBlock.get(1) + " and " + itemsInBlock.get(2);
      }
      deleteBlockForm
          .withId("block-delete-form")
          .with(
              div(
                      h1(join(blockDefinition.name(), " includes ", b(listItemsInBlock + ".")))
                          .withClasses("text-base", "mb-2"),
                      h1("Are you sure you want to delete this screen?")
                          .withClasses("text-base", "mb-2"))
                  .withClasses("mx-4"),
              submitButton("Delete")
                  .withId("delete-block-button")
                  .withClasses(
                      "mx-4", "my-1", "inline", "opacity-100", StyleUtils.disabled("opacity-50")));
    }

    ButtonTag deleteScreenButton =
        ViewUtils.makeSvgTextButton("Delete screen", Icons.DELETE)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES);

    return Modal.builder("block-delete-modal", deleteBlockForm)
        .setModalTitle(String.format("Delete %s?", blockDefinition.name()))
        .setTriggerButtonContent(deleteScreenButton)
        .setWidth(Modal.Width.THIRD)
        .build();
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

  private boolean viewAllowsEditingProgram() {
    return programDisplayType.equals(DRAFT);
  }

  private ButtonTag renderEditButton(Request request, ProgramDefinition programDefinition) {
    if (viewAllowsEditingProgram()) {
      ButtonTag editButton = getStandardizedEditButton("Edit program details");
      String editLink = routes.AdminProgramController.edit(programDefinition.id()).url();
      return asRedirectElement(editButton, editLink);
    } else {
      ButtonTag editButton = getStandardizedEditButton("Edit program");
      String editLink = routes.AdminProgramController.newVersionFrom(programDefinition.id()).url();
      return toLinkButtonForPost(editButton, editLink, request);
    }
  }

  @Override
  protected ProgramDisplayType getProgramDisplayStatus() {
    return programDisplayType;
  }

  public interface Factory {
    ProgramBlockEditView create(ProgramDisplayType type);
  }
}
