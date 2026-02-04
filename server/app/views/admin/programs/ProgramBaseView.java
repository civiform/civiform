package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static views.style.AdminStyles.HEADER_BUTTON_STYLES;

import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import controllers.admin.routes;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OlTag;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import models.CategoryModel;
import play.mvc.Http;
import services.program.ProgramDefinition;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateUseCase;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.components.Icons;
import views.components.TextFormatter;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

abstract class ProgramBaseView extends BaseHtmlView {

  /** Represents different buttons that can be displayed in the program information header. */
  public enum ProgramHeaderButton {
    /** Redirect to the program api bridge definition editing page. */
    EDIT_BRIDGE_DEFINITIONS,
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
    DOWNLOAD_PDF_PREVIEW
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
                TextFormatter.formatTextForAdmins(
                    programDefinition.localizedDescription().getDefault()))
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
            categoriesDiv,
            headerButtonsDiv)
        .withClasses("bg-gray-100", "text-gray-800", "shadow-md", "p-8", "pt-4", "-mx-2");
  }

  /** Renders a div actively indicating there is no predicate condition for the admin. */
  protected final DivTag renderEmptyPredicate(
      PredicateUseCase predicateUseCase, long programId, long blockId, boolean includeEditFooter) {
    DivTag emptyPredicateStatus =
        div().withClasses("border", "border-gray-200", "p-4", "usa-prose", "my-2");
    String message =
        switch (predicateUseCase) {
          case ELIGIBILITY -> "This screen does not have any eligibility conditions.";
          case VISIBILITY -> "This screen is always shown.";
        };
    emptyPredicateStatus.with(div().withText(message));

    if (!includeEditFooter) {
      return emptyPredicateStatus;
    }
    ATag editLink = a().withClasses("usa-link");
    if (predicateUseCase == PredicateUseCase.ELIGIBILITY) {
      editLink
          .withId(ReferenceClasses.EDIT_ELIGIBILITY_PREDICATE_LINK)
          .withHref(
              routes.AdminProgramBlockPredicatesController.editEligibility(programId, blockId)
                  .url())
          .withText("Add eligibility conditions");
    } else if (predicateUseCase == PredicateUseCase.VISIBILITY) {
      editLink
          .withId(ReferenceClasses.EDIT_VISIBILITY_PREDICATE_LINK)
          .withHref(
              routes.AdminProgramBlockPredicatesController.editVisibility(programId, blockId).url())
          .withText("Add visibility conditions");
    }
    return emptyPredicateStatus.with(div().with(editLink));
  }

  /** Renders a div presenting the predicate definition for the admin. */
  protected final DivTag renderExistingPredicate(
      long programId,
      long blockId,
      String blockName,
      PredicateDefinition predicateDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions,
      PredicateUseCase predicateUseCase,
      boolean includeEditFooter,
      boolean expanded,
      boolean expandedFormLogicEnabled) {
    DivTag header =
        div()
            .with(
                TagCreator.button()
                    .withClasses(
                        "usa-accordion__button",
                        "flex",
                        "p-4",
                        "gap-4",
                        "items-center",
                        "text-black",
                        "font-normal",
                        "bg-transparent")
                    .withType("button")
                    .attr("aria-expanded", expanded)
                    .attr(
                        "aria-controls",
                        predicateUseCase.name().toLowerCase(Locale.ROOT) + "-content")
                    .condWith(
                        predicateUseCase == PredicateUseCase.ELIGIBILITY,
                        Icons.svg(Icons.HOW_TO_REG)
                            .withClasses("w-6", "h-5", "shrink-0")
                            .attr("role", "img")
                            .attr("aria-hidden", "true"),
                        p("This screen has eligibility conditions.").withClass("flex-grow"))
                    .condWith(
                        predicateUseCase == PredicateUseCase.VISIBILITY,
                        Icons.svg(Icons.VISIBILITY_OFF)
                            .withClasses("w-6", "h-5", "shrink-0")
                            .attr("role", "img")
                            .attr("aria-hidden", "true"),
                        p("This screen has visibility conditions.").withClass("flex-grow")));

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            blockName, predicateDefinition, questionDefinitions, expandedFormLogicEnabled);
    DivTag content =
        div()
            .withId(predicateUseCase.name().toLowerCase(Locale.ROOT) + "-content")
            .withClasses("prose-body", "px-4", "pb-4")
            .with(p(readablePredicate.formattedHtmlHeading()));
    if (readablePredicate.formattedHtmlConditionList().isPresent()) {
      OlTag conditionList = ol().withClasses("list-decimal", "ml-4", "pt-4");
      readablePredicate.formattedHtmlConditionList().get().stream()
          .forEach(condition -> conditionList.with(li(condition)));
      content.with(conditionList);
    }

    DivTag container =
        div()
            .withClasses(
                "my-2",
                "border",
                "border-gray-200",
                "gap-4",
                "items-center",
                "usa-accordion",
                StyleUtils.hover("text-gray-800", "bg-gray-100"))
            .with(header, content);

    if (!includeEditFooter) {
      return container;
    }
    DivTag footer =
        div()
            .withClasses("prose-body", "px-4", "pb-4")
            .condWith(
                predicateUseCase == PredicateUseCase.ELIGIBILITY,
                a().withHref(
                        routes.AdminProgramBlockPredicatesController.editEligibility(
                                programId, blockId)
                            .url())
                    .withText("Edit eligibility conditions")
                    .withClasses("usa-link")
                    .withId(ReferenceClasses.EDIT_ELIGIBILITY_PREDICATE_LINK))
            .condWith(
                predicateUseCase == PredicateUseCase.VISIBILITY,
                a().withHref(
                        routes.AdminProgramBlockPredicatesController.editVisibility(
                                programId, blockId)
                            .url())
                    .withText("Edit visibility conditions")
                    .withClasses("usa-link")
                    .withId(ReferenceClasses.EDIT_VISIBILITY_PREDICATE_LINK));
    return container.with(footer);
  }

  private ButtonTag renderHeaderButton(
      ProgramHeaderButton headerButton, ProgramDefinition programDefinition, Http.Request request) {
    return switch (headerButton) {
      case EDIT_PROGRAM -> {
        ButtonTag editButton = getStandardizedEditButton("Edit program");
        String editLink =
            routes.AdminProgramController.newVersionFrom(programDefinition.id()).url();
        yield toLinkButtonForPost(editButton, editLink, request);
      }
      case EDIT_PROGRAM_DETAILS ->
          asRedirectElement(
              getStandardizedEditButton("Edit program details"),
              routes.AdminProgramController.edit(
                      programDefinition.id(), ProgramEditStatus.EDIT.name())
                  .url());
      case EDIT_PROGRAM_IMAGE ->
          asRedirectElement(
              ViewUtils.makeSvgTextButton("Edit program image", Icons.IMAGE)
                  .withClasses(HEADER_BUTTON_STYLES)
                  .withId("header_edit_program_image_button"),
              routes.AdminProgramImageController.index(
                      programDefinition.id(), ProgramEditStatus.EDIT.name())
                  .url());
      case PREVIEW_AS_APPLICANT ->
          asRedirectElement(
              ViewUtils.makeSvgTextButton("Preview as applicant", Icons.VIEW)
                  .withClasses(HEADER_BUTTON_STYLES),
              routes.AdminProgramPreviewController.preview(programDefinition.slug()).url());
      case DOWNLOAD_PDF_PREVIEW ->
          asRedirectElement(
              ViewUtils.makeSvgTextButton("Download PDF preview", Icons.DOWNLOAD)
                  .withClasses(HEADER_BUTTON_STYLES),
              routes.AdminProgramPreviewController.pdfPreview(programDefinition.id()).url());

      case EDIT_BRIDGE_DEFINITIONS ->
          asRedirectElement(
              ViewUtils.makeSvgTextButton("Edit Bridge Definition", Icons.CAKE)
                  .withClasses(HEADER_BUTTON_STYLES),
              controllers.admin.apibridge.routes.ProgramBridgeController.edit(
                      programDefinition.id())
                  .url());
    };
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
