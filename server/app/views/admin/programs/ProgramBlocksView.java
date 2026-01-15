package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.b;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.input;
import static j2html.TagCreator.join;
import static j2html.TagCreator.label;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import controllers.admin.routes;
import forms.BlockForm;
import j2html.TagCreator;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.UlTag;
import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.MessageKey;
import services.ProgramBlockValidationFactory;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateUseCase;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import services.settings.SettingsManifest;
import views.AlertComponent;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.QuestionCard;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.Modal;
import views.components.ProgramQuestionBank;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Renders a page for an admin to view or edit the configuration for a program, including a list of
 * all blocks to select from and a detailed view of the selected program.
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
 * The read only version contains mostly the same elements, but without any of the edit controls.
 */
public final class ProgramBlocksView extends ProgramBaseView {

  private static final Logger logger = LoggerFactory.getLogger(ProgramBlocksView.class);

  private final AdminLayout layout;
  private final ProgramDisplayType programDisplayType;
  private final ProgramBlockValidationFactory programBlockValidationFactory;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  public static final String BLOCK_TYPE_FORM_FIELD = "blockType";
  public static final String MOVE_QUESTION_POSITION_FIELD = "position";
  private static final String CREATE_BLOCK_FORM_ID = "block-create-form";
  private static final String CREATE_REPEATED_BLOCK_FORM_ID = "repeated-block-create-form";
  private static final String CREATE_ENUMERATOR_BLOCK_FORM_ID = "enumerator-block-create-form";
  private static final String DELETE_BLOCK_FORM_ID = "block-delete-form";
  private static final int BASE_INDENTATION_SIZE = 4;
  private static final int INDENTATION_FACTOR_INCREASE_ON_LEVEL = 2;
  private static final String QUESTIONS_SECTION_ID = "questions-section";

  @Inject
  public ProgramBlocksView(
      ProgramBlockValidationFactory programBlockValidationFactory,
      @Assisted ProgramDisplayType programViewType,
      AdminLayoutFactory layoutFactory,
      SettingsManifest settingsManifest) {
    super(settingsManifest);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.programDisplayType = programViewType;
    this.programBlockValidationFactory = checkNotNull(programBlockValidationFactory);
  }

  public Content render(
      Request request,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      Optional<ToastMessage> message,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions,
      Messages messages) {
    return render(
        request,
        program,
        blockDefinition.id(),
        new BlockForm(blockDefinition.name(), blockDefinition.description()),
        blockDefinition,
        blockDefinition.programQuestionDefinitions(),
        message,
        questions,
        allPreviousVersionQuestions,
        messages);
  }

  public Content render(
      Request request,
      ProgramDefinition programDefinition,
      long blockId,
      BlockForm blockForm,
      BlockDefinition blockDefinition,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      Optional<ToastMessage> message,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions,
      Messages messages) {
    InputTag csrfTag = makeCsrfTokenInputTag(request);

    String title =
        viewAllowsEditingProgram()
            ? String.format("Edit %s", blockDefinition.name())
            : String.format("View %s", blockDefinition.name());
    Long programId = programDefinition.id();

    String blockUpdateAction =
        controllers.admin.routes.AdminProgramBlocksController.update(programId, blockId).url();
    Modal blockDescriptionEditModal =
        renderBlockDescriptionModal(csrfTag, blockForm, blockUpdateAction);

    String blockDeleteAction =
        controllers.admin.routes.AdminProgramBlocksController.delete(programId, blockId).url();
    Modal blockDeleteScreenModal =
        renderBlockDeleteModal(csrfTag, blockDeleteAction, blockDefinition);

    boolean malformedQuestionDefinition =
        programDefinition.getNonRepeatedBlockDefinitions().stream()
            .anyMatch(BlockDefinition::hasNullQuestion);

    ArrayList<ProgramHeaderButton> headerButtons =
        new ArrayList<>(
            getEditHeaderButtons(/* isEditingAllowed= */ viewAllowsEditingProgram(), request));

    // External programs applications are hosted outside of Civiform. Therefore, we shouldn't show
    // buttons to preview or download the application.
    if (programDefinition.programType() != ProgramType.EXTERNAL) {
      headerButtons.add(ProgramHeaderButton.PREVIEW_AS_APPLICANT);
      headerButtons.add(ProgramHeaderButton.DOWNLOAD_PDF_PREVIEW);
    }

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
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
                        renderProgramInfoHeader(
                                programDefinition, ImmutableList.copyOf(headerButtons), request)
                            .with(
                                iff(
                                    malformedQuestionDefinition,
                                    div(
                                        p("If you see this file a bug with the CiviForm development"
                                                + " team. Some questions are not pointing at the"
                                                + " latest version. Edit the program and try"
                                                + " republishing. ")
                                            .withClasses("text-center", "text-red-500")))),
                        // External programs applications are hosted outside of Civiform. Therefore,
                        // we shouldn't show the block panel since there are no application
                        // questions.
                        iff(
                            programDefinition.programType() != ProgramType.EXTERNAL,
                            div()
                                .withClasses("flex", "flex-grow", "-mx-2")
                                .withData("testid", "block-panel")
                                .with(
                                    renderBlockOrderPanel(
                                        request, programDefinition, blockId, messages))
                                .with(
                                    renderBlockPanel(
                                        programDefinition,
                                        blockDefinition,
                                        blockForm,
                                        blockQuestions,
                                        questions,
                                        allPreviousVersionQuestions,
                                        blockDefinition.hasEnumeratorQuestion(),
                                        csrfTag,
                                        blockDescriptionEditModal.getButton(),
                                        blockDeleteScreenModal.getButton(),
                                        request,
                                        messages)))));

    // Add top level UI that is only visible in the editable version.
    if (viewAllowsEditingProgram()) {
      htmlBundle
          .addMainContent(
              renderQuestionBankPanel(
                  questions,
                  programDefinition,
                  blockDefinition,
                  csrfTag,
                  ProgramQuestionBank.shouldShowQuestionBank(request),
                  request))
          .addMainContent(addFormEndpoints(csrfTag, programDefinition.id(), blockId))
          .addModals(blockDescriptionEditModal, blockDeleteScreenModal);
    }

    // Add toast messages
    request
        .flash()
        .get("error")
        .map(ToastMessage::errorNonLocalized)
        .ifPresent(htmlBundle::addToastMessages);
    message.ifPresent(htmlBundle::addToastMessages);

    return layout.render(htmlBundle);
  }

  /**
   * Returns the header buttons used for editing various parts of the program (details, image,
   * etc.).
   *
   * @param isEditingAllowed true if the view allows editing and false otherwise. (Typically, a view
   *     only allows editing if a program is in draft mode.)
   */
  private ImmutableList<ProgramHeaderButton> getEditHeaderButtons(
      boolean isEditingAllowed, Request request) {
    if (isEditingAllowed) {
      if (settingsManifest.getApiBridgeEnabled(request)) {
        return ImmutableList.of(
            ProgramHeaderButton.EDIT_PROGRAM_DETAILS,
            ProgramHeaderButton.EDIT_PROGRAM_IMAGE,
            ProgramHeaderButton.EDIT_BRIDGE_DEFINITIONS);
      }

      return ImmutableList.of(
          ProgramHeaderButton.EDIT_PROGRAM_DETAILS, ProgramHeaderButton.EDIT_PROGRAM_IMAGE);
    } else {
      return ImmutableList.of(ProgramHeaderButton.EDIT_PROGRAM);
    }
  }

  private DivTag addFormEndpoints(InputTag csrfTag, long programId, long blockId) {
    String blockCreateAction =
        controllers.admin.routes.AdminProgramBlocksController.create(programId).url();
    FormTag createBlockForm =
        form(csrfTag)
            .withId(CREATE_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction)
            .with(
                FieldWithLabel.input()
                    .setFieldName(BLOCK_TYPE_FORM_FIELD)
                    .setValue(BlockType.SINGLE.toString())
                    .getInputTag());

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
                    .getNumberTag())
            .with(
                FieldWithLabel.input()
                    .setFieldName(BLOCK_TYPE_FORM_FIELD)
                    .setValue(BlockType.REPEATED.toString())
                    .getInputTag());

    FormTag createRepeatedSetForm =
        form(csrfTag)
            .withId(CREATE_ENUMERATOR_BLOCK_FORM_ID)
            .withMethod(HttpVerbs.POST)
            .withAction(blockCreateAction)
            .with(
                FieldWithLabel.input()
                    .setFieldName(BLOCK_TYPE_FORM_FIELD)
                    .setValue(BlockType.ENUMERATOR.toString())
                    .getInputTag());

    return div(createBlockForm, createRepeatedBlockForm, createRepeatedSetForm)
        .withClasses("hidden");
  }

  /**
   * Returns a wrapper panel that contains the list of all blocks and, if the view shows an editable
   * program, a button to add a new screen,
   */
  private DivTag renderBlockOrderPanel(
      Request request, ProgramDefinition program, long focusedBlockId, Messages messages) {
    DivTag ret = div().withClasses("shadow-lg", "pt-6", "w-2/12", "border-r", "border-gray-200");
    ret.with(
        renderBlockList(
            request,
            program,
            program.getNonRepeatedBlockDefinitions(),
            focusedBlockId,
            /* level= */ 0,
            messages));

    if (viewAllowsEditingProgram()) {
      ret.condWith(
          !settingsManifest.getEnumeratorImprovementsEnabled(request),
          ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
              .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "m-4")
              .withType("submit")
              .withId("add-block-button")
              .withForm(CREATE_BLOCK_FORM_ID));
      ret.condWith(
          settingsManifest.getEnumeratorImprovementsEnabled(request),
          ViewUtils.makeSvgTextButton("Add screen", Icons.ADD)
              .withId("add-screen")
              .attr("aria-controls", "add-screen-dropdown")
              .attr("aria-expanded", "false")
              .withClasses(
                  ButtonStyles.OUTLINED_WHITE_WITH_ICON, "m-4", ReferenceClasses.WITH_DROPDOWN),
          ul().withId("add-screen-dropdown")
              .withClasses(
                  "hidden", "border", "border-gray-10", "margin-left-205", "margin-right-4")
              .with(
                  li(
                      button("Add screen")
                          .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN, "width-full")
                          .withType("submit")
                          .withId("add-block-button")
                          .withForm(CREATE_BLOCK_FORM_ID)),
                  li(
                      button(messages.at(MessageKey.BUTTON_REPEATED_SET_ADD_NEW.getKeyName()))
                          .withClasses(ButtonStyles.CLEAR_WITH_ICON_FOR_DROPDOWN, "width-full")
                          .withType("submit")
                          .withId("add-enumerator-block-button")
                          .withForm(CREATE_ENUMERATOR_BLOCK_FORM_ID))));
    }
    return ret;
  }

  /**
   * Returns a panel that shows all Blocks of the given program. In an editable view it also adds a
   * button that allows to add a new screen and controls to change the order.
   */
  private DivTag renderBlockList(
      Request request,
      ProgramDefinition programDefinition,
      ImmutableList<BlockDefinition> blockDefinitions,
      long focusedBlockId,
      int level,
      Messages messages) {
    DivTag container = div();
    String genericBlockDivId = "block_list_item_";
    for (BlockDefinition blockDefinition : blockDefinitions) {
      // TODO: Not i18n safe.
      int numQuestions = blockDefinition.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);
      if (settingsManifest.getEnumeratorImprovementsEnabled(request)
          && blockDefinition.getIsEnumerator()) {
        questionCountText =
            (level > 0)
                ? messages.at(MessageKey.TEXT_NESTED_REPEATED_SET.getKeyName())
                : messages.at(MessageKey.TEXT_REPEATED_SET.getKeyName());
      }
      String blockName = blockDefinition.name();
      // indentation value for enums and repeaters
      int listIndentationFactor = level * INDENTATION_FACTOR_INCREASE_ON_LEVEL;
      DivTag blockContent =
          div()
              .withClasses(
                  "flex",
                  "flex-row",
                  "gap-2",
                  "py-2",
                  "mr-0", // style for tablet and mobile
                  "lg:mr-4", // style for desktop
                  "ml-" + listIndentationFactor, // style for tablet and mobile
                  "lg:ml-" + (BASE_INDENTATION_SIZE + listIndentationFactor), // style for desktop
                  "max-w-md");
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
      // Show icon with blocks that have visibility conditions.
      // Icon is always added for spacing, but is only visible for blocks that have visibility
      // conditions.
      String showOrHideVisibilityIcon =
          blockDefinition.visibilityPredicate().isEmpty() ? "invisible" : "";
      blockContent
          .withId(genericBlockDivId + blockDefinition.id())
          .with(
              a().withClasses(
                      "w-5",
                      "h-5",
                      "mr-0", // style for tablet and mobile
                      "lg:mr-2", // style for desktop
                      "self-center",
                      "flex-shrink-0",
                      showOrHideVisibilityIcon)
                  .withHref(switchBlockLink)
                  .with(Icons.svg(Icons.VISIBILITY_OFF)))
          .with(
              a().withClasses("flex-grow", "overflow-hidden")
                  .withHref(switchBlockLink)
                  .with(
                      p(blockName)
                          .withClass(iff(blockDefinition.hasNullQuestion(), "text-red-500")),
                      p(questionCountText).withClasses("text-sm")));
      if (viewAllowsEditingProgram()) {
        DivTag moveButtons =
            renderBlockMoveButtons(
                request, programDefinition.id(), blockDefinitions, blockDefinition);
        blockContent.with(moveButtons);
      }
      String selectedClasses = blockDefinition.id() == focusedBlockId ? "bg-info-light" : "";
      DivTag blockContainer =
          div()
              .withClasses(
                  "border",
                  "border-white",
                  "max-w-md",
                  StyleUtils.hover("border-gray-300"),
                  selectedClasses);
      container.with(blockContainer.with(blockContent));

      // Recursively add repeated blocks indented under their enumerator block
      if (blockDefinition.getIsEnumerator() || blockDefinition.hasEnumeratorQuestion()) {
        container.with(
            renderBlockList(
                request,
                programDefinition,
                programDefinition.getBlockDefinitionsForEnumerator(blockDefinition.id()),
                focusedBlockId,
                level + 1,
                messages));
      }
    }
    return container;
  }

  /**
   * Creates a set of buttons, which are shown next to each block in the list of blocks. They are
   * used to move a block up or down in the list.
   */
  private DivTag renderBlockMoveButtons(
      Request request,
      long programId,
      ImmutableList<BlockDefinition> blockDefinitions,
      BlockDefinition blockDefinition) {
    String moveUpFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move up button is invisible for the first block
    String moveUpInvisible =
        blockDefinition.id() == blockDefinitions.get(0).id() ? "invisible" : "";
    String moveUpTestId = "move-block-up-" + blockDefinition.id();
    DivTag moveUp =
        div()
            .withClass(moveUpInvisible)
            .with(
                form()
                    .withAction(moveUpFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.UP.name()))
                    .with(
                        submitButton("^")
                            .withClasses(AdminStyles.MOVE_BLOCK_BUTTON)
                            .attr("data-test-id", moveUpTestId)));

    String moveDownFormAction =
        routes.AdminProgramBlocksController.move(programId, blockDefinition.id()).url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
        blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1).id()
            ? "invisible"
            : "";
    String moveDownTestId = "move-block-down-" + blockDefinition.id();
    DivTag moveDown =
        div()
            .withClasses("transform", "rotate-180", moveDownInvisible)
            .with(
                form()
                    .withAction(moveDownFormAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(request))
                    .with(input().isHidden().withName("direction").withValue(Direction.DOWN.name()))
                    .with(
                        submitButton("^")
                            .withClasses(AdminStyles.MOVE_BLOCK_BUTTON)
                            .attr("data-test-id", moveDownTestId)));
    return div().withClasses("flex", "flex-col", "self-center").with(moveUp, moveDown);
  }

  /**
   * Returns the Div that contains all details about the specified block. Tends to be used to show
   * the details of the currently selected block for viewing or editing.
   */
  private DivTag renderBlockPanel(
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      BlockForm blockForm,
      ImmutableList<ProgramQuestionDefinition> blockQuestions,
      ImmutableList<QuestionDefinition> allQuestions,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions,
      boolean blockDefinitionHasEnumeratorQuestion,
      InputTag csrfTag,
      ButtonTag blockDescriptionModalButton,
      ButtonTag blockDeleteModalButton,
      Request request,
      Messages messages) {
    // A block can only be deleted when it has no repeated blocks. Same is true for
    // removing the enumerator question from the block.
    final boolean canDelete =
        !blockDefinitionHasEnumeratorQuestion || hasNoRepeatedBlocks(program, blockDefinition.id());

    DivTag blockInfoDisplay =
        div()
            .with(div(blockForm.getName()).withClasses("text-xl", "font-bold", "py-2", "break-all"))
            .with(
                div(blockForm.getDescription()).withClasses("text-lg", "max-w-prose", "break-all"))
            .withId("block-info-display-" + blockDefinition.id())
            .withClasses("my-4");

    DivTag visibilityPredicateDisplay =
        renderVisibilityPredicate(
            program.id(),
            blockDefinition.id(),
            blockDefinition.visibilityPredicate(),
            blockDefinition.name(),
            allQuestions,
            settingsManifest.getExpandedFormLogicEnabled(request));

    Optional<DivTag> maybeEligibilityPredicateDisplay = Optional.empty();
    if (!program.programType().equals(ProgramType.COMMON_INTAKE_FORM)) {
      maybeEligibilityPredicateDisplay =
          Optional.of(
              renderEligibilityPredicate(
                  program,
                  blockDefinition.id(),
                  blockDefinition.eligibilityDefinition(),
                  blockDefinition.name(),
                  allQuestions,
                  settingsManifest.getExpandedFormLogicEnabled(request)));
    }

    // Precompute a map of questions to block ids that use the question in visibility conditions.
    // This will be used to render related visibility conditions in each question card.
    ImmutableSetMultimap.Builder<Long, Long> questionIdToVisibilityBlockIdBuilder =
        ImmutableSetMultimap.builder();
    program.blockDefinitions().stream()
        .filter(block -> block.visibilityPredicate().isPresent())
        .forEach(
            block ->
                block.visibilityPredicate().get().getQuestions().stream()
                    .forEach(
                        questionId ->
                            questionIdToVisibilityBlockIdBuilder.put(questionId, block.id())));
    ImmutableSetMultimap<Long, Long> questionIdToVisibilityBlockIdMap =
        questionIdToVisibilityBlockIdBuilder.build();

    DivTag programQuestions =
        div()
            .withId(QUESTIONS_SECTION_ID)
            .withClasses("my-4")
            .with(div("Questions").withClasses("text-lg", "font-bold", "py-2"));

    IntStream.range(0, blockQuestions.size())
        .forEach(
            index -> {
              ProgramQuestionDefinition question = blockQuestions.get(index);
              QuestionDefinition questionDefinition =
                  findQuestionDefinition(question, allPreviousVersionQuestions);

              programQuestions.with(
                  renderQuestion(
                      csrfTag,
                      program,
                      blockDefinition,
                      questionDefinition,
                      canDelete,
                      question.optional(),
                      question.addressCorrectionEnabled(),
                      index,
                      blockQuestions.size(),
                      question.getQuestionDefinition() instanceof NullQuestionDefinition,
                      request,
                      questionIdToVisibilityBlockIdMap.get(questionDefinition.getId())));
            });

    DivTag div = div().withClasses("w-7/12", "py-6", "px-4").withData("testId", "block-panel-edit");

    // UI elements for editing are only needed when we view a draft
    if (viewAllowsEditingProgram()) {
      DivTag buttons =
          renderBlockPanelButtons(
              program,
              blockDefinitionHasEnumeratorQuestion,
              blockDescriptionModalButton,
              blockDeleteModalButton,
              canDelete);
      ButtonTag addQuestion =
          makeSvgTextButton("Add a question", Icons.ADD)
              .withClasses(
                  ButtonStyles.SOLID_BLUE_WITH_ICON,
                  ReferenceClasses.OPEN_QUESTION_BANK_BUTTON,
                  "my-4");

      div.with(blockInfoDisplay, buttons, visibilityPredicateDisplay);
      maybeEligibilityPredicateDisplay.ifPresent(div::with);

      if (settingsManifest.getEnumeratorImprovementsEnabled(request)
          && blockDefinition.getIsEnumerator()) {
        return div.with(
            renderEnumeratorScreenContent(
                blockDefinitionHasEnumeratorQuestion,
                csrfTag,
                programQuestions,
                addQuestion,
                messages));
      }

      return div.with(programQuestions, addQuestion);
    }

    div.with(blockInfoDisplay, visibilityPredicateDisplay);
    maybeEligibilityPredicateDisplay.ifPresent(div::with);
    return div.with(programQuestions);
  }

  /**
   * Returns the QuestionDefinition to display. If we find a NullQuestionDefinition we will attempt
   * to find it in the list of previous version questions.
   *
   * <p>If we still don't find the details we'll return the NullQuestionDefinition. The screen will
   * render a box indicating there's an error, but won't have details to show
   */
  private QuestionDefinition findQuestionDefinition(
      ProgramQuestionDefinition question,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions) {
    QuestionDefinition questionDefinition = question.getQuestionDefinition();

    if (!(questionDefinition instanceof NullQuestionDefinition)) {
      return questionDefinition;
    }

    var foundMissingQuestionDefinition =
        allPreviousVersionQuestions.stream().filter(x -> x.getId() == question.id()).findFirst();

    return foundMissingQuestionDefinition.orElse(questionDefinition);
  }

  private DivTag renderEnumeratorScreenContent(
      boolean blockDefinitionHasEnumeratorQuestion,
      InputTag csrfTag,
      DivTag programQuestions,
      ButtonTag addQuestion,
      Messages messages) {
    // If it's an empty enumerator block
    if (!blockDefinitionHasEnumeratorQuestion) {
      FieldsetTag creationMethodRadio = renderCreationMethodRadioButtons(messages);
      FormTag newEnumeratorQuestionForm = renderNewEnumeratorQuestionForm(csrfTag, messages);
      return div().with(creationMethodRadio, newEnumeratorQuestionForm);
    } else {
      return div()
          .with(
              div("This is an enumerator block that already has a question."),
              programQuestions,
              addQuestion);
    }
  }

  private FieldsetTag renderCreationMethodRadioButtons(Messages messages) {
    return fieldset(
        legend(messages.at(MessageKey.HEADING_REPEATED_SET_CREATION_METHOD.getKeyName()))
            .withClass("text-gray-600")
            .with(ViewUtils.requiredQuestionIndicator()),
        div()
            .withClass("usa-radio")
            .with(
                input()
                    .withType("radio")
                    .withName("creation-method-option")
                    .withId("create-new")
                    .withValue("create-new")
                    .withClass("usa-radio__input usa-radio__input--tile"),
                label(messages.at(MessageKey.OPTION_REPEATED_SET_CREATE_NEW.getKeyName()))
                    .withClass("usa-radio__label")
                    .attr("for", "create-new")),
        div()
            .withClass("usa-radio")
            .with(
                input()
                    .withType("radio")
                    .withName("creation-method-option")
                    .withId("choose-existing")
                    .withValue("choose-existing")
                    .withClass("usa-radio__input usa-radio__input--tile"),
                label(messages.at(MessageKey.OPTION_REPEATED_SET_CHOOSE_EXISTING.getKeyName()))
                    .withClass("usa-radio__label")
                    .attr("for", "choose-existing"))
            .withClasses("usa-fieldset"));
  }

  private FormTag renderNewEnumeratorQuestionForm(InputTag csrfTag, Messages messages) {
    return form(csrfTag)
        .withClasses("border", "border-gray-300")
        .withId("new-enumerator-question-form")
        .withMethod(HttpVerbs.POST)
        .with(
            p(messages.at(MessageKey.LABEL_NEW_REPEATED_SET_FORM.getKeyName())),
            FieldWithLabel.input()
                .setId("listed-entity-input")
                .setFieldName("listed-entity")
                .setLabelText(messages.at(MessageKey.INPUT_LISTED_ENTITY.getKeyName()))
                .getUSWDSInputTag(),
            FieldWithLabel.input()
                .setId("enumerator-admin-id-input")
                .setFieldName("enumerator-admin-id")
                .setLabelText(messages.at(MessageKey.INPUT_REPEATED_SET_ADMIN_ID.getKeyName()))
                .getUSWDSInputTag(),
            FieldWithLabel.textArea()
                .setId("question-text-input")
                .setFieldName("question-text")
                .setLabelText(messages.at(MessageKey.INPUT_REPEATED_SET_QUESTION_TEXT.getKeyName()))
                .getUSWDSTextareaTag(),
            FieldWithLabel.textArea()
                .setId("hint-text-input")
                .setFieldName("hint")
                .setLabelText(messages.at(MessageKey.INPUT_REPEATED_SET_HINT_TEXT.getKeyName()))
                .getUSWDSTextareaTag(),
            FieldWithLabel.number()
                .setId("min-entity-count-input")
                .setFieldName("min-entity-count")
                .setLabelText(messages.at(MessageKey.INPUT_REPEATED_SET_MIN_ENTITIES.getKeyName()))
                .getNumberTag(),
            FieldWithLabel.number()
                .setId("max-entity-count-input")
                .setFieldName("max-entity-count")
                .setLabelText(messages.at(MessageKey.INPUT_REPEATED_SET_MAX_ENTITIES.getKeyName()))
                .getNumberTag(),
            AlertComponent.renderSlimInfoAlert(
                messages.at(MessageKey.ALERT_REPEATED_SET_NEW_QUESTION.getKeyName())),
            submitButton(messages.at(MessageKey.BUTTON_REPEATED_SET_SUBMIT_NEW.getKeyName()))
                .withId("create-repeated-set-button")
                .withClasses("usa-button", "usa-button--primary"));
  }

  private DivTag renderBlockPanelButtons(
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
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON));

    // TODO: Maybe add alpha variants to button color on hover over so we do not
    // have to hard-code what the color will be when button is in hover state?

    // Only add the delete button if there is more than one screen in the program
    if (program.blockDefinitions().size() > 1) {
      buttons.with(div().withClass("flex-grow"));
      blockDeleteModalButton
          .withCondDisabled(!canDelete)
          .withCondTitle(
              !canDelete, "A screen can only be deleted when it has no repeated screens.");
      buttons.with(blockDeleteModalButton);
    }
    return buttons;
  }

  /** Creates the UI that is used to display or edit the visibility setting of a specified block. */
  private DivTag renderVisibilityPredicate(
      long programId,
      long blockId,
      Optional<PredicateDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions,
      boolean expandedFormLogicEnabled) {
    DivTag div =
        div()
            .withId("visibility-predicate")
            .withClasses("my-4")
            .with(div("Visibility condition").withClasses("text-lg", "font-bold", "py-2"));
    if (predicate.isEmpty()) {
      return div.with(
          renderEmptyPredicate(
              PredicateUseCase.VISIBILITY,
              programId,
              blockId,
              /* includeEditFooter= */ viewAllowsEditingProgram()));
    } else {
      return div.with(
          renderExistingPredicate(
              programId,
              blockId,
              blockName,
              predicate.get(),
              questions,
              PredicateUseCase.VISIBILITY,
              /* includeEditFooter= */ viewAllowsEditingProgram(),
              /* expanded= */ false,
              expandedFormLogicEnabled));
    }
  }

  /**
   * Creates the UI that is used to display or edit the eligibility setting of a specified block.
   */
  private DivTag renderEligibilityPredicate(
      ProgramDefinition program,
      long blockId,
      Optional<EligibilityDefinition> predicate,
      String blockName,
      ImmutableList<QuestionDefinition> questions,
      boolean expandedFormLogicEnabled) {
    DivTag div =
        div()
            .withId("eligibility-predicate")
            .withClasses("my-4")
            .with(div("Eligibility condition").withClasses("text-lg", "font-bold", "py-2"))
            .with(
                renderEmptyEligibilityPredicate(program, viewAllowsEditingProgram())
                    .withClasses("text-lg", "max-w-prose"));
    if (predicate.isEmpty()) {
      return div.with(
          renderEmptyPredicate(
              PredicateUseCase.ELIGIBILITY,
              program.id(),
              blockId,
              /* includeEditFooter= */ viewAllowsEditingProgram()));
    } else {
      return div.with(
          renderExistingPredicate(
              program.id(),
              blockId,
              blockName,
              predicate.get().predicate(),
              questions,
              PredicateUseCase.ELIGIBILITY,
              /* includeEditFooter= */ viewAllowsEditingProgram(),
              /* expanded= */ false,
              /* expandedFormLogicEnabled= */ expandedFormLogicEnabled));
    }
  }

  private DivTag renderEmptyEligibilityPredicate(
      ProgramDefinition program, boolean editingAllowed) {
    ImmutableList.Builder<DomContent> emptyPredicateContentBuilder = ImmutableList.builder();
    String eligibilityText =
        program.eligibilityIsGating()
            ? "Applicants who do not meet the minimum requirements will be blocked from submitting"
                + " an application."
            : "Applicants can submit an application even if they do not meet the minimum"
                + " requirements.";
    emptyPredicateContentBuilder
        .add(
            text(
                "You can add eligibility conditions to determine if an applicant qualifies for the"
                    + " program. "))
        .add(text(eligibilityText));

    if (editingAllowed) {
      emptyPredicateContentBuilder
          .add(text(" You can change this in the "))
          .add(
              a().withData("testid", "goto-program-settings-link")
                  .withText("program settings.")
                  .withHref(
                      routes.AdminProgramController.edit(
                              program.id(), ProgramEditStatus.EDIT.name())
                          .url())
                  .withClasses(BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT));
    } else {
      emptyPredicateContentBuilder.add(
          text(" You can change this in the program settings if your program is in draft mode."));
    }
    return div().with(emptyPredicateContentBuilder.build());
  }

  /**
   * Renders an individual question, including the description and any toggles or tags that should
   * be shown next to the question in the list of questions.
   */
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
      boolean malformedQuestionDefinition,
      Request request,
      ImmutableSet<Long> visibilityGatedBlockIds) {
    ImmutableList.Builder<DomContent> rowContent = ImmutableList.builder();
    Optional<FormTag> maybeAddressCorrectionEnabledToggle =
        renderAddressCorrectionEnabledToggle(
            request,
            csrfTag,
            programDefinition,
            blockDefinition,
            questionDefinition,
            addressCorrectionEnabled);
    Optional<FormTag> maybeOptionalToggle =
        renderOptionalToggle(
            csrfTag, programDefinition.id(), blockDefinition.id(), questionDefinition, isOptional);
    // UI for editing is only added if we are viewing a draft.
    if (viewAllowsEditingProgram()) {
      if (maybeAddressCorrectionEnabledToggle.isPresent()) {
        rowContent.add(maybeAddressCorrectionEnabledToggle.get());
      }
      if (maybeOptionalToggle.isPresent()) {
        rowContent.add(maybeOptionalToggle.get());
      }
      rowContent.add(
          renderMoveQuestionButtonsSection(
              csrfTag,
              programDefinition.id(),
              blockDefinition.id(),
              questionDefinition,
              questionIndex,
              questionsCount));
      rowContent.add(
          div()
              .with(renderEditQuestionLink(questionDefinition.getId()))
              .with(
                  renderDeleteQuestionForm(
                      csrfTag,
                      programDefinition.id(),
                      blockDefinition.id(),
                      questionDefinition,
                      canRemove))
              .withClasses("flex", "flex-column"));
    } else {
      // For each toggle, use a label instead in the read only view
      if (maybeAddressCorrectionEnabledToggle.isPresent()) {
        String label =
            addressCorrectionEnabled
                ? "Address correction: enabled"
                : "Address correction: disabled";
        rowContent.add(renderReadOnlyLabel(label));
      }
      if (maybeOptionalToggle.isPresent()) {
        String label = isOptional ? "optional question" : "required question";
        rowContent.add(renderReadOnlyLabel(label));
      }
    }
    Optional<DivTag> visibilityAccordion =
        visibilityGatedBlockIds.isEmpty()
            ? Optional.empty()
            : Optional.of(
                renderVisibilityAccordion(
                    questionDefinition, programDefinition, visibilityGatedBlockIds));
    return QuestionCard.renderForProgramPage(
        questionDefinition, malformedQuestionDefinition, rowContent.build(), visibilityAccordion);
  }

  /**
   * Creates a Divtag that contains an up and a down arrow. Those are displayed next to a question
   * to allow changing the order of questions inside a block.
   */
  private DivTag renderMoveQuestionButtonsSection(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      int questionIndex,
      int questionsCount) {
    FormTag moveUp =
        createMoveQuestionButton(
            csrfTag,
            programDefinitionId,
            blockDefinitionId,
            questionDefinition,
            Math.max(0, questionIndex - 1),
            Icons.ARROW_UPWARD,
            /* label= */ "move up",
            /* isInvisible= */ questionIndex == 0);
    FormTag moveDown =
        createMoveQuestionButton(
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

  /**
   * Returns a form tag containing one up or down button (used for ordering questions inside a
   * program block).
   */
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

  /**
   * Optionally creates a form tag which contains a toggle that allows to specify if a question is
   * optional or mandatory.
   */
  private Optional<FormTag> renderOptionalToggle(
      InputTag csrfTag,
      long programDefinitionId,
      long blockDefinitionId,
      QuestionDefinition questionDefinition,
      boolean isOptional) {
    if (questionDefinition instanceof StaticContentQuestionDefinition) {
      return Optional.empty();
    }

    String toggleOptionalFormId = String.format("toggle-optional-%d", questionDefinition.getId());
    String toggleOptionalAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.setOptional(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
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
            .withType("button")
            .attr("hx-post", toggleOptionalAction)
            .attr("hx-select-oob", String.format("#%s", toggleOptionalFormId))
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
    return Optional.of(
        form(csrfTag)
            .withId(toggleOptionalFormId)
            .with(input().isHidden().withName("optional").withValue(isOptional ? "false" : "true"))
            .with(optionalButton));
  }

  /** Creates label that can be used in the read only view, for example to replace a toggle. */
  private DivTag renderReadOnlyLabel(String label) {
    DivTag ret =
        div()
            .withClasses(
                "flex", "gap-2", "items-center", "text-gray-400", "font-medium", "rounded-full")
            .with(p(label));
    return ret;
  }

  /**
   * Returns an Optional of a form that contains the toggle which allows enabling address correction
   * for address related questions.
   */
  private Optional<FormTag> renderAddressCorrectionEnabledToggle(
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
    boolean addressCorrectionEnabledQuestionAlreadyExists =
        blockDefinition.hasAddressCorrectionEnabledOnDifferentQuestion(questionDefinition.getId());
    String toolTipText =
        "Enabling 'address correction' will check the resident's address to ensure it is accurate.";

    toolTipText +=
        addressCorrectionEnabledQuestionAlreadyExists
            ? " This screen already contains a question with address correction enabled. This"
                + " feature can only be enabled once per screen."
            : " You can select one address question to correct per screen.";

    if (questionIsUsedInPredicate) {
      toolTipText +=
          " Questions used in visibility or eligibility conditions must have address correction"
              + " enabled.";
    }

    DivTag toolTip;
    if (!settingsManifest.getEsriAddressCorrectionEnabled(request)) {
      // Leave the space at the end, because we will add a "Learn more" link. This
      // should always be the last string added to toolTipText for this reason.
      toolTipText +=
          " To use this feature, you will need to have your IT manager configure the GIS service. ";
      toolTip =
          ViewUtils.makeSvgToolTipRightAnchoredWithLink(
              toolTipText,
              Icons.INFO,
              "Learn more",
              "https://docs.civiform.us/it-manual/sre-playbook/configure-gis-service");
    } else {
      toolTip = ViewUtils.makeSvgToolTipRightAnchored(toolTipText, Icons.INFO);
    }

    String toggleAddressCorrectionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController
            .toggleAddressCorrectionEnabledState(
                programDefinition.id(), blockDefinition.id(), questionDefinition.getId())
            .url();
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
            .withType("button")
            .attr("hx-post", toggleAddressCorrectionAction)
            // Replace entire Questions section so that the tooltips for all address
            // questions get updated.
            .attr("hx-select-oob", String.format("#%s", QUESTIONS_SECTION_ID))
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
            .with(div(toolTip));
    return Optional.of(
        form(csrfTag)
            .with(
                input()
                    .isHidden()
                    .withName("addressCorrectionEnabled")
                    .withValue(String.valueOf(addressCorrectionEnabled)))
            .with(addressCorrectionButton));
  }

  private ATag renderEditQuestionLink(Long questionId) {
    return a("Edit")
        .withHref(routes.AdminQuestionController.edit(questionId).url())
        .withClasses("usa-link", "pb-2", "self-center");
  }

  /**
   * Returns a form that shows the delete button, which is to be shown next to each of the questions
   * in a program block to allow deleting the question.
   */
  private FormTag renderDeleteQuestionForm(
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
                ButtonStyles.OUTLINED_WHITE_WITH_ICON,
                canRemove ? "" : "opacity-50");
    String deleteQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.delete(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
    return form(csrfTag)
        .withId("block-questions-form")
        .withMethod(HttpVerbs.POST)
        .withAction(deleteQuestionAction)
        .with(removeButton);
  }

  /**
   * Renders accordion that may be shown at the bottom of a question card if this question is used
   * in visibility conditions. Includes the block name and links to edit the visibility conditions
   * gated by this question.
   */
  private DivTag renderVisibilityAccordion(
      QuestionDefinition questionDefinition,
      ProgramDefinition programDefinition,
      ImmutableSet<Long> visibilityGatedBlockIds) {
    DivTag visibilityHeader =
        div()
            .with(
                TagCreator.button()
                    .withClasses(
                        "usa-accordion__button",
                        "flex",
                        "gap-4",
                        "items-center",
                        "bg-transparent",
                        "text-black",
                        "font-normal")
                    .withType("button")
                    .attr("aria-expanded", "false")
                    .attr("aria-controls", questionDefinition.getName() + "-visibility-content")
                    .with(
                        Icons.svg(Icons.VISIBILITY_OFF).withClasses("w-6", "h-5", "shrink-0"),
                        p("This question shows or hides screens.").withClass("flex-grow")));
    UlTag editVisibilityList = ul().withClasses("list-disc", "ml-4");
    visibilityGatedBlockIds.forEach(
        blockId -> {
          try {
            editVisibilityList.with(
                li(
                    a().withHref(
                            routes.AdminProgramBlockPredicatesController.editVisibility(
                                    programDefinition.id(), blockId)
                                .url())
                        .withText(programDefinition.getBlockDefinition(blockId).name())
                        .withClasses("usa-link")));
          } catch (ProgramBlockDefinitionNotFoundException e) {
            // Log and skip if block definition can't be found.
            // This is safe to ignore and proceed gracefully since this is a non-critical part of
            // the page view.
            logger.error("Program block not found: {}", e);
          }
        });
    DivTag visibilityContent =
        div()
            .withId(questionDefinition.getName() + "-visibility-content")
            .withClasses("pl-14", "pb-2")
            .with(
                p("Edit related visibility conditions by clicking the below link(s):"),
                editVisibilityList);
    DivTag visibilityAccordion =
        div()
            .withId(questionDefinition.getName() + "-visibility-accordion")
            .withClasses("bg-gray-100", "border-gray-300", "usa-accordion")
            .with(visibilityHeader, visibilityContent);
    return visibilityAccordion;
  }

  /** Creates the question panel, which shows all questions the admin can add to a program. */
  private DivTag renderQuestionBankPanel(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ProgramDefinition program,
      BlockDefinition blockDefinition,
      InputTag csrfTag,
      ProgramQuestionBank.Visibility questionBankVisibility,
      Request request) {
    String addQuestionAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.create(
                program.id(), blockDefinition.id())
            .url();

    String redirectUrl =
        ProgramQuestionBank.addShowQuestionBankParam(
            controllers.admin.routes.AdminProgramBlocksController.edit(
                    program.id(), blockDefinition.id())
                .url());
    ProgramQuestionBank qb =
        new ProgramQuestionBank(
            ProgramQuestionBank.ProgramQuestionBankParams.builder()
                .setQuestionAction(addQuestionAction)
                .setCsrfTag(csrfTag)
                .setQuestions(questionDefinitions)
                .setProgram(program)
                .setBlockDefinition(blockDefinition)
                .setQuestionCreateRedirectUrl(redirectUrl)
                .build(),
            programBlockValidationFactory,
            settingsManifest,
            request);
    return qb.getContainer(questionBankVisibility);
  }

  /** Creates a modal, which allows the admin to confirm that they want to delete a block. */
  private Modal renderBlockDeleteModal(
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

    // If there are no questions, eligibilty conditions, or visibility conditions on
    // this screen, just print "Are you sure you want to delete this screen?"
    if (itemsInBlock.size() == 0) {
      deleteBlockForm
          .withId("block-delete-form")
          .with(
              div(div("Are you sure you want to delete this screen?").withClasses("mb-4")),
              submitButton("Delete")
                  .withId("delete-block-button")
                  .withClasses("my-1", "inline", "opacity-100", StyleUtils.disabled("opacity-50")));
    } else {
      // If there are questions, eligibility conditions, or visibility conditions on
      // this screen,
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
                  div(join(blockDefinition.name(), " includes ", b(listItemsInBlock + ".")))
                      .withClasses("mb-2"),
                  div("Are you sure you want to delete this screen?").withClasses("mb-4")),
              submitButton("Delete")
                  .withId("delete-block-button")
                  .withClasses("my-1", "inline", "opacity-100", StyleUtils.disabled("opacity-50")));
    }

    ButtonTag deleteScreenButton =
        ViewUtils.makeSvgTextButton("Delete screen", Icons.DELETE)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON);

    return Modal.builder()
        .setModalId("block-delete-modal")
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(deleteBlockForm)
        .setModalTitle(String.format("Delete %s?", blockDefinition.name()))
        .setTriggerButtonContent(deleteScreenButton)
        .setWidth(Modal.Width.THIRD)
        .build();
  }

  /**
   * Creates a Modal that is used for viewing or editing the Description field of the specified
   * block.
   */
  private Modal renderBlockDescriptionModal(
      InputTag csrfTag, BlockForm blockForm, String blockUpdateAction) {
    String modalTitle = "Screen name and description";
    FormTag blockDescriptionForm =
        form(csrfTag).withMethod(HttpVerbs.POST).withAction(blockUpdateAction);
    blockDescriptionForm
        .withId("block-edit-form")
        .with(
            div(
                    h1("The screen name and description will help a user understand which part of"
                            + " an application they are on.")
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
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON);
    return Modal.builder()
        .setModalId("block-description-modal")
        .setLocation(Modal.Location.ADMIN_FACING)
        .setContent(blockDescriptionForm)
        .setModalTitle(modalTitle)
        .setTriggerButtonContent(editScreenButton)
        .setWidth(Modal.Width.THIRD)
        .build();
  }

  private boolean hasNoRepeatedBlocks(ProgramDefinition programDefinition, long blockId) {
    return programDefinition.getBlockDefinitionsForEnumerator(blockId).isEmpty();
  }

  /** Returns if this view is editable or not. A view is editable only if it represents a draft. */
  private boolean viewAllowsEditingProgram() {
    return programDisplayType.equals(DRAFT);
  }

  /** Indicates if this view is showing a draft or published program. */
  @Override
  protected ProgramDisplayType getProgramDisplayStatus() {
    return programDisplayType;
  }

  public interface Factory {
    ProgramBlocksView create(ProgramDisplayType type);
  }
}
