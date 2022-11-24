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
import j2html.tags.DomContent;
import j2html.tags.Tag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.IntStream;
import models.Program;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.applicant.question.Question;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDefinition.Direction;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
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
 * Renders a page for an admin to edit the configuration for a single block of a
 * program. A block is a synonym for a Screen.
 **/
public class ProgramBlockViewOnlyView extends ProgramBlockView {

  private final AdminLayout layout;
  private final ArrayList<Modal> modals;

  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  public static final String MOVE_QUESTION_POSITION_FIELD = "position";

  @Inject
  public ProgramBlockViewOnlyView(AdminLayoutFactory layoutFactory,
    Config config) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    // this.featureFlagOptionalQuestions = checkNotNull(config).hasPath("cf.optional_questions");
    this.modals = new ArrayList<>();
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
      new BlockForm(blockDefinition.name(), blockDefinition.description()),
      blockDefinition,
      message,
      questions);
  }

  public Content render(
    Request request,
    ProgramDefinition programDefinition,
    BlockForm blockForm,
    BlockDefinition blockDefinition,
    Optional<ToastMessage> message,
    ImmutableList<QuestionDefinition> questions) {

    String title = String.format("View %s", blockDefinition.name());

    HtmlBundle htmlBundle =
      layout
        .getBundle()
        .setTitle(title)
        .addMainContent(mainContent(request, programDefinition, blockForm, blockDefinition, questions).toArray(new Tag[0]))
        .addModals(modals);

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

  protected ArrayList<Tag> mainContent(
    Request request,
    ProgramDefinition programDefinition,
    BlockForm blockForm,
    BlockDefinition blockDefinition,
    ImmutableList<QuestionDefinition> questions) {

    DivTag mainContent =
      div()
        .withClasses(
          "flex",
          "flex-grow",
          "flex-col",
          "px-2",
          StyleUtils.responsive2XLarge("px-16"))
        .with(
          renderProgramInfo(programDefinition),
          div()
            .withClasses("flex", "flex-grow", "-mx-2")
            .with(blockOrderPanel(request, programDefinition, blockDefinition.id()))
            .with(
              blockPanel(
                blockDefinition,
                blockForm,
                questions )));
    return new ArrayList<>(Arrays.asList(mainContent));
  }

  /**
   * Define the String that will be shown on the Edit button
   **/
  @Override
  protected String getEditButtonText() {
    return "Edit program";
  }

  @Override
  protected BadgeStatus getBadgeStatus() {
    return BadgeStatus.ACTIVE;
  }

  // TODO(jhummel) adapt the url to lead to edit page

  /**
   * Define the navigation destination for the Edit button
   **/
  @Override
  protected String getNavigationUrl(ProgramDefinition programDefinition) {
    return routes.AdminProgramController.edit(programDefinition.id()).url();
  }

  private DivTag blockOrderPanel(Request request, ProgramDefinition program,
    long focusedBlockId) {
    DivTag ret = div().withClasses("shadow-lg", "pt-6", "w-2/12", "border-r",
      "border-gray-200");
    return ret.with(
      renderBlockList(request, program,
        program.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));
  }


  // TODO(jhummel) remove the move buttons
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
      String questionCountText = String.format("Question count: %d",
        numQuestions);
      String blockName = blockDefinition.name();

      DivTag moveButtons =
        blockMoveButtons(request, programDefinition.id(), blockDefinitions,
          blockDefinition);
      String selectedClasses =
        blockDefinition.id() == focusedBlockId ? "bg-gray-100" : "";
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
            programDefinition.getBlockDefinitionsForEnumerator(
              blockDefinition.id()),
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
      routes.AdminProgramBlocksController.move(programId, blockDefinition.id())
        .url();
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
            .with(input().isHidden().withName("direction")
              .withValue(Direction.UP.name()))
            .with(
              submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));

    String moveDownFormAction =
      routes.AdminProgramBlocksController.move(programId, blockDefinition.id())
        .url();
    // Move down button is invisible for the last block
    String moveDownInvisible =
      blockDefinition.id() == blockDefinitions.get(blockDefinitions.size() - 1)
        .id()
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
            .with(input().isHidden().withName("direction")
              .withValue(Direction.DOWN.name()))
            .with(
              submitButton("^").withClasses(AdminStyles.MOVE_BLOCK_BUTTON)));
    DivTag moveButtons =
      div().withClasses("flex", "flex-col", "self-center")
        .with(moveUp, moveDown);
    return moveButtons;
  }


  private ArrayList<DomContent> prepareContentForBlockPanel(
    // ProgramDefinition program,
    BlockDefinition blockDefinition,
    BlockForm blockForm,
    ImmutableList<QuestionDefinition> allQuestions
  ) {

    ImmutableList<ProgramQuestionDefinition> blockQuestions = blockDefinition.programQuestionDefinitions();

    DivTag blockInfoDisplay =
      div()
        .with(
          div(blockForm.getName()).withClasses("text-xl", "font-bold", "py-2"))
        .with(
          div(blockForm.getDescription()).withClasses("text-lg", "max-w-prose"))
        .withClasses("my-4");

    DivTag predicateDisplay =
      renderPredicate(
        //program.id(),
        blockDefinition,
        allQuestions);

    DivTag programQuestions = div();
    IntStream.range(0, blockQuestions.size())
      .forEach(
        index -> {
          var question = blockQuestions.get(index);
          programQuestions.with(
            renderQuestion(
              // program,
              question
              // index,
              // blockQuestions.size()
            ));
        });

    return new ArrayList<DomContent>(
      Arrays.asList(blockInfoDisplay, predicateDisplay, programQuestions));
  }

  private DivTag blockPanel(
    // ProgramDefinition program,
    BlockDefinition blockDefinition,
    BlockForm blockForm,
    ImmutableList<QuestionDefinition> allQuestions
  ) {
    //
    ArrayList<DomContent> content = prepareContentForBlockPanel(blockDefinition,
      blockForm,
      allQuestions);
    return div()
      .withClasses("w-7/12", "py-6", "px-4")
      .with(content);
  }

  private DivTag renderPredicate(
    // long programId,
    BlockDefinition blockDefinition,
    ImmutableList<QuestionDefinition> questions) {

    Optional<PredicateDefinition> predicate = blockDefinition.visibilityPredicate();
    String currentBlockStatus =
      predicate.isEmpty()
        ? "This screen is always shown."
        : predicate.get().toDisplayString(blockDefinition.name(), questions);

    return div()
      .withClasses("my-4")
      .with(
        div("Visibility condition").withClasses("text-lg", "font-bold", "py-2"))
      .with(div(currentBlockStatus).withClasses("text-lg", "max-w-prose"));
  }

  // ps

  private DivTag renderQuestion(
    // ProgramDefinition programDefinition,
    ProgramQuestionDefinition question
    // int questionIndex,
//     int questionsCount
  ) {

    QuestionDefinition questionDefinition = question.getQuestionDefinition();
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
    return ret.with(icon, content);
  }
}
