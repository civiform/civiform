package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.li;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static views.style.AdminStyles.HEADER_BUTTON_STYLES;

import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.UlTag;
import java.util.List;
import java.util.stream.Collectors;
import models.CategoryModel;
import play.mvc.Http;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
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
    /**
     * Downloads a PDF preview of the current program version, with all of its blocks and questions.
     */
    DOWNLOAD_PDF_PREVIEW,
  }

  protected final SettingsManifest settingsManifest;

  public ProgramBaseView(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
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
            .withClasses("text-sm", "mb-2");
    DivTag adminNote =
        div()
            .withClasses("text-sm", "mb-2")
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

    DivTag categoriesDiv =
        div(
                span("Categories: ").withClasses("font-semibold"),
                iffElse(
                    programDefinition.categories().isEmpty(),
                    span("None"),
                    span(
                        programDefinition.categories().stream()
                            .map(CategoryModel::getDefaultName)
                            .collect(Collectors.joining(", ")))))
            .withClasses("text-sm");

    return div(
            ViewUtils.makeLifecycleBadge(getProgramDisplayStatus()),
            title,
            description,
            adminNote,
            iff(settingsManifest.getProgramFilteringEnabled(request), categoriesDiv),
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

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            blockName, predicateDefinition, questionDefinitions);

    container.with(text(readablePredicate.heading()));
    if (readablePredicate.conditionList().isPresent()) {
      UlTag conditionList = ul().withClasses("list-disc", "ml-4", "mb-4");
      readablePredicate.conditionList().get().stream()
          .forEach(condition -> conditionList.with(li(condition)));
      container.with(conditionList);
    }
    return container;
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
            routes.AdminProgramController.edit(
                    programDefinition.id(), ProgramEditStatus.EDIT.name())
                .url());
      case EDIT_PROGRAM_IMAGE:
        return asRedirectElement(
            ViewUtils.makeSvgTextButton("Edit program image", Icons.IMAGE)
                .withClasses(HEADER_BUTTON_STYLES)
                .withId("header_edit_program_image_button"),
            routes.AdminProgramImageController.index(
                    programDefinition.id(), ProgramEditStatus.EDIT.name())
                .url());
      case PREVIEW_AS_APPLICANT:
        return asRedirectElement(
            ViewUtils.makeSvgTextButton("Preview as applicant", Icons.VIEW)
                .withClasses(HEADER_BUTTON_STYLES),
            routes.AdminProgramPreviewController.preview(programDefinition.id()).url());
      case DOWNLOAD_PDF_PREVIEW:
        return asRedirectElement(
            ViewUtils.makeSvgTextButton("Download PDF preview", Icons.DOWNLOAD)
                .withClasses(HEADER_BUTTON_STYLES),
            routes.AdminProgramPreviewController.pdfPreview(programDefinition.id()).url());
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
