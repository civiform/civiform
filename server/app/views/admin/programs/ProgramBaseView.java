package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static views.style.AdminStyles.HEADER_BUTTON_STYLES;

import com.google.common.collect.ImmutableList;
import controllers.admin.AdminProgramImageController;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import java.util.List;
import play.mvc.Http;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.components.Icons;
import views.components.TextFormatter;
import views.style.StyleUtils;

abstract class ProgramBaseView extends BaseHtmlView {

  /** Represents different buttons that can be displayed in the program information header. */
  public enum ProgramHeaderButton {
    /**
     * Redirects program to an editable view. Should be used only if the program is currently read
     * only.
     */
    EDIT_PROGRAM,
    /** Redirects to the program details editing page. */
    EDIT_PROGRAM_DETAILS,
    /** Redirects to the program image editing page. */
    EDIT_PROGRAM_IMAGE,
    /** Redirects to previewing this program as an applicant. */
    PREVIEW_AS_APPLICANT,
  }

  /**
   * Returns the header buttons used for editing various parts of the program (details, image,
   * etc.).
   *
   * @param isEditingAllowed true if the view allows editing and false otherwise. (Typically, a view
   *     only allows editing if a program is in draft mode.)
   */
  protected final ImmutableList<ProgramHeaderButton> getEditHeaderButtons(
      Http.Request request, SettingsManifest settingsManifest, boolean isEditingAllowed) {
    if (isEditingAllowed) {
      if (settingsManifest.getProgramCardImages(request)) {
        return ImmutableList.of(
            ProgramHeaderButton.EDIT_PROGRAM_DETAILS, ProgramHeaderButton.EDIT_PROGRAM_IMAGE);
      } else {
        return ImmutableList.of(ProgramHeaderButton.EDIT_PROGRAM_DETAILS);
      }
    } else {
      return ImmutableList.of(ProgramHeaderButton.EDIT_PROGRAM);
    }
  }

  /**
   * Renders a header div with internal/admin program information.
   *
   * @param headerButtons the main action buttons to be displayed in the header
   * @throws IllegalArgumentException if {@code headerButtons} contains both {@link
   *     ProgramHeaderButton#EDIT_PROGRAM} and {@link ProgramHeaderButton#EDIT_PROGRAM_DETAILS}.
   */
  protected final DivTag renderProgramInfoHeader(
      ProgramDefinition programDefinition,
      List<ProgramHeaderButton> headerButtons,
      Http.Request request) {
    if (headerButtons.contains(ProgramHeaderButton.EDIT_PROGRAM)
        && headerButtons.contains(ProgramHeaderButton.EDIT_PROGRAM_DETAILS)) {
      throw new IllegalArgumentException(
          "At most one of [EDIT_PROGRAM, EDIT_PROGRAM_DETAILS] should be included");
    }
    DivTag title =
        div(programDefinition.localizedName().getDefault())
            .withId("program-title")
            .withClasses("text-3xl", "pb-3");
    DivTag description =
        div()
            .with(
                TextFormatter.formatText(
                    programDefinition.localizedDescription().getDefault(),
                    /* preserveEmptyLines= */ false,
                    /* addRequiredIndicator= */ false))
            .withClasses("text-sm");
    DivTag adminNote =
        div()
            .withClasses("text-sm")
            .with(span("Admin note: ").withClasses("font-semibold"))
            .with(span(programDefinition.adminDescription()));
    DivTag headerButtonsDiv =
        div()
            .withClasses("flex")
            .with(
                headerButtons.stream()
                    .map(
                        headerButton ->
                            renderHeaderButton(headerButton, programDefinition, request)));
    return div(
            ViewUtils.makeLifecycleBadge(getProgramDisplayStatus()),
            title,
            description,
            adminNote,
            headerButtonsDiv)
        .withClasses("bg-gray-100", "text-gray-800", "shadow-md", "p-8", "pt-4", "-mx-2");
  }

  /** Renders a div presenting the predicate definition for the admin. */
  protected final DivTag renderExistingPredicate(
      String blockName,
      PredicateDefinition predicateDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions) {
    DivTag container =
        div()
            .withClasses(
                "my-2",
                "border",
                "border-gray-200",
                "px-4",
                "py-4",
                "gap-4",
                "items-center",
                StyleUtils.hover("text-gray-800", "bg-gray-100"));

    if (predicateDefinition
        .predicateFormat()
        .equals(PredicateDefinition.PredicateFormat.SINGLE_QUESTION)) {
      return container.with(
          text(predicateDefinition.toDisplayString(blockName, questionDefinitions)));
    } else if (!predicateDefinition
        .predicateFormat()
        .equals(PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS)) {
      throw new IllegalArgumentException(
          String.format(
              "Predicate type %s is unsupported.", predicateDefinition.predicateFormat()));
    }

    ImmutableList<PredicateExpressionNode> andNodes =
        predicateDefinition.rootNode().getOrNode().children();

    if (andNodes.size() == 1) {
      return container.with(
          text(
              blockName
                  + " is "
                  + predicateDefinition.action().toDisplayString()
                  + " "
                  + andNodes.get(0).getAndNode().toDisplayString(questionDefinitions)));
    }

    container.with(
        text(blockName + " is " + predicateDefinition.action().toDisplayString() + " any of:"));
    UlTag conditionList = ul().withClasses("list-disc", "ml-4", "mb-4");

    andNodes.stream()
        .map(PredicateExpressionNode::getAndNode)
        .forEach(andNode -> conditionList.with(li(andNode.toDisplayString(questionDefinitions))));

    return container.with(conditionList);
  }

  private ButtonTag renderHeaderButton(
      ProgramHeaderButton headerButton, ProgramDefinition programDefinition, Http.Request request) {
    switch (headerButton) {
      case EDIT_PROGRAM:
        ButtonTag editButton = getStandardizedEditButton("Edit program");
        String editLink =
            routes.AdminProgramController.newVersionFrom(programDefinition.id()).url();
        return toLinkButtonForPost(editButton, editLink, request);
      case EDIT_PROGRAM_DETAILS:
        return asRedirectElement(
            getStandardizedEditButton("Edit program details"),
            routes.AdminProgramController.edit(programDefinition.id()).url());
      case EDIT_PROGRAM_IMAGE:
        return asRedirectElement(
            ViewUtils.makeSvgTextButton("Edit program image", Icons.IMAGE)
                .withClasses(HEADER_BUTTON_STYLES)
                .withId("header_edit_program_image_button"),
            routes.AdminProgramImageController.index(
                    programDefinition.id(), AdminProgramImageController.Referer.BLOCKS.name())
                .url());
      case PREVIEW_AS_APPLICANT:
        return asRedirectElement(
            ViewUtils.makeSvgTextButton("Preview as applicant", Icons.VIEW)
                .withClasses(HEADER_BUTTON_STYLES),
            routes.AdminProgramPreviewController.preview(programDefinition.id()).url());
      default:
        throw new IllegalStateException("All header buttons handled");
    }
  }

  private ButtonTag getStandardizedEditButton(String buttonText) {
    return ViewUtils.makeSvgTextButton(buttonText, Icons.EDIT)
        .withClasses(HEADER_BUTTON_STYLES)
        .withId("header_edit_button");
  }

  /**
   * Returns the Program display type which represents the status of the program. It will be shown
   * at the top of the page.
   */
  protected abstract ProgramDisplayType getProgramDisplayStatus();
}
