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
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils.BadgeStatus;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.components.SvgTag;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Renders a view only page for an admin to see the details of an active program,
 * including a list of all screens and details about the block they select.
 * A block is a synonym for a screen.
 **/
public class ProgramBlockViewOnlyView extends ProgramBlockView {

  private final AdminLayout layout;
  public static final String ENUMERATOR_ID_FORM_FIELD = "enumeratorId";
  public static final String MOVE_QUESTION_POSITION_FIELD = "position";

  @Inject
  public ProgramBlockViewOnlyView(AdminLayoutFactory layoutFactory,
    Config config) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
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

    HtmlBundle htmlBundle = createHtmlBundle(request, programDefinition, blockForm, blockDefinition, questions);
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

  protected HtmlBundle createHtmlBundle(Request request, ProgramDefinition programDefinition, BlockForm blockForm,
    BlockDefinition blockDefinition,  ImmutableList<QuestionDefinition> questions) {
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
                programDefinition,
                blockDefinition,
                blockForm,
                questions )));

    return layout
      .getBundle()
      .setTitle(String.format("View %s", blockDefinition.name()))
      .addMainContent(mainContent);
  }

  /*
  * Creates the panel that shows a list of all screens and allows reordering them as well as
  * selecting one to see the details of the screen definition.
   */
  protected DivTag blockOrderPanel(Request request, ProgramDefinition programDefinition,
    long focusedBlockId) {
    return div()
      .withClasses("shadow-lg", "pt-6", "w-2/12", "border-r",
      "border-gray-200")
      .with(blockList(request, programDefinition, programDefinition.getNonRepeatedBlockDefinitions(), focusedBlockId, 0));
  }

  /*
  * Recursively renders a list of all blocks of a given program to be shown in the block
  * order panel.
   */
  private DivTag blockList(
    Request request,
    ProgramDefinition programDefinition,
    ImmutableList<BlockDefinition> blockDefinitions,
    long focusedBlockId,
    int level) {
    DivTag container = div().withClass("pl-" + level * 2);
    int index = 0;
    for (BlockDefinition blockDefinition : blockDefinitions) {
      container.with(blockTag(blockDefinitions, index++, programDefinition, focusedBlockId));

      // Recursively add repeated blocks indented under their enumerator block
      if (blockDefinition.isEnumerator()) {
        container.with(
          blockList(
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

  /*
  * Creates a Div that represents an individual block in the  list of blocks shown in the
  * block order panel.
   */
  protected DivTag blockTag (ImmutableList<BlockDefinition>  blockDefinitions,
    int blockIndex, ProgramDefinition programDefinition /* needed for subclasses */,
    long focusedBlockId) {
     //
    BlockDefinition blockDefinition =blockDefinitions.get(blockIndex);
    int numQuestions = blockDefinition.getQuestionCount();
    String selectionColoring =
      blockDefinition.id() == focusedBlockId ? "bg-gray-100" : "";
    String questionCountText = String.format("Question count: %d", numQuestions);

    String blockLink =
      controllers.admin.routes.AdminProgramBlocksController.edit(
          programDefinition.id(), blockDefinition.id())
        .url();

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
          selectionColoring)
        .with(
          a().withClasses("flex-grow", "overflow-hidden")
            .withHref(blockLink)
            .with(p(blockDefinition.name()), p(questionCountText).withClasses("text-sm")));
      return blockTag;
  }

  private DivTag blockPanel(
    ProgramDefinition programDefinition,
    BlockDefinition blockDefinition,
    BlockForm blockForm,
    ImmutableList<QuestionDefinition> allQuestions) {
    ArrayList<DomContent> content = prepareContentForBlockPanel(programDefinition, blockDefinition,
      blockForm,
      allQuestions);
    return div()
      .withClasses("w-7/12", "py-6", "px-4")
      .with(content);
  }

  /**
   * Creates a list of elements that will be shown in the block panel.
   * The list format allows subclasses to add elements to the list at various indices before the content is added to the UI.
   */
  protected ArrayList<DomContent> prepareContentForBlockPanel(
    ProgramDefinition program,
    BlockDefinition blockDefinition,
    BlockForm blockForm,
    ImmutableList<QuestionDefinition> allQuestions) {

    DivTag blockInfoDisplay =
      div()
        .with(
          div(blockForm.getName()).withClasses("text-xl", "font-bold", "py-2"))
        .with(
          div(blockForm.getDescription()).withClasses("text-lg", "max-w-prose"))
        .withClasses("my-4");

    DivTag predicateDisplay =
      renderPredicate(
        blockDefinition,
        allQuestions);

    ImmutableList<ProgramQuestionDefinition> blockQuestions = blockDefinition.programQuestionDefinitions();
    DivTag programQuestions = div();
    IntStream.range(0, blockQuestions.size())
      .forEach(
        questionIndex -> {
          programQuestions.with(
            renderQuestion(
              program,
              blockDefinition,
              questionIndex));
        });
    return new ArrayList<DomContent>(
      Arrays.asList(blockInfoDisplay, predicateDisplay, programQuestions));
  }

  private DivTag renderPredicate(
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

  /**
   * Renders an individual question to be shown in the Block panel.
   */
  protected DivTag renderQuestion(
    ProgramDefinition programDefinition /* needed for subclasses */,
    BlockDefinition blockDefinition,
    int questionIndex) {
    ImmutableList<ProgramQuestionDefinition> blockQuestions = blockDefinition.programQuestionDefinitions();
    ProgramQuestionDefinition question = blockQuestions.get(questionIndex);

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

  @Override
  protected String getEditButtonText() {
    return "Edit program";
  }

  // TODO when the ProgramBlockViewOnlyView is used, clicking the button should lead to the
  // ProgramBlockEditView.
  @Override
  protected String getButtonUrl(ProgramDefinition programDefinition) {
    return null;
   }

  @Override
  protected BadgeStatus getBadgeStatus() {
    return BadgeStatus.ACTIVE;
  }
}
