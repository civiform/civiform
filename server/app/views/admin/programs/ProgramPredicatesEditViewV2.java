package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.text;
import static play.mvc.Http.HttpVerbs.POST;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LabelTag;
import java.util.Locale;
import java.util.UUID;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.HtmlBundle;
import views.ViewUtils.ProgramDisplayType;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.LinkElement.IconPosition;
import views.components.ToastMessage;
import views.style.ReferenceClasses;

/** Renders a page for editing predicates of a block in a program. */
public final class ProgramPredicatesEditViewV2 extends ProgramBaseView {

  private final AdminLayout layout;

  // The functionality type of the predicate editor.
  public enum ViewType {
    ELIGIBILITY,
    VISIBILITY
  }

  @Inject
  public ProgramPredicatesEditViewV2(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /**
   * Renders the Predicate editor.
   *
   * <p>The UI:
   *
   * <ul>
   *   <li>Shows the current predicate
   *   <li>Allows for removing (/destroy handler) the existing predicate.
   *   <li>Presents options to set (/update handler) a new predicate for each question in {@code
   *       predicateQuestions}.
   * </ul>
   *
   * <p>Only one predicate can exist on a block so the UI does a full replace operation when
   * setting/updating a predicate.
   */
  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      ImmutableList<QuestionDefinition> predicateQuestions,
      ViewType type) {

    // This render code is used to render eligibility and visibility predicate editors.
    // The following vars set the per-type visual and url values and the rest lays things out
    // identically for the most part.

    final String predicateTypeNameTitleCase;
    final String h2CurrentCondition;
    final DivTag existingPredicateDisplay;
    final String textNewCondition;
    final String textNoAvailableQuestions;
    final String removePredicateUrl;
    final String configureExistingPredicateUrl;
    final String configureNewPredicateUrl;
    final boolean hasExistingPredicate;

    switch (type) {
      case ELIGIBILITY:
        predicateTypeNameTitleCase = "Eligibility";
        h2CurrentCondition = "Current eligibility condition";
        textNewCondition =
            "You can select the questions you would like to add eligibility conditions to. When"
                + " you create an eligibility condition, it replaces the present one.";
        textNoAvailableQuestions =
            "There are no available questions with which to set an eligibility condition for this"
                + " screen.";
        hasExistingPredicate = blockDefinition.eligibilityDefinition().isPresent();
        existingPredicateDisplay =
            blockDefinition
                .eligibilityDefinition()
                .map(EligibilityDefinition::predicate)
                .map(
                    pred ->
                        div()
                            .with(
                                renderExistingPredicate(
                                    blockDefinition.name(), pred, predicateQuestions)))
                .orElse(div("This screen is always eligible."));
        removePredicateUrl =
            routes.AdminProgramBlockPredicatesController.destroyEligibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        configureExistingPredicateUrl =
            routes.AdminProgramBlockPredicatesController.configureExistingEligibilityPredicate(
                    programDefinition.id(), blockDefinition.id())
                .url();
        configureNewPredicateUrl =
            routes.AdminProgramBlockPredicatesController.configureNewEligibilityPredicate(
                    programDefinition.id(), blockDefinition.id())
                .url();
        break;
      case VISIBILITY:
        predicateTypeNameTitleCase = "Visibility";
        h2CurrentCondition = "Current visibility condition";
        textNewCondition =
            "You can select the questions you would like to add visibility conditions to. When you"
                + " create a visibility condition, it replaces the present one.";
        textNoAvailableQuestions =
            "There are no available questions with which to set a visibility condition for this"
                + " screen.";
        hasExistingPredicate = blockDefinition.visibilityPredicate().isPresent();
        existingPredicateDisplay =
            blockDefinition
                .visibilityPredicate()
                .map(
                    pred ->
                        renderExistingPredicate(blockDefinition.name(), pred, predicateQuestions))
                .orElse(div("This screen is always shown."));
        removePredicateUrl =
            routes.AdminProgramBlockPredicatesController.destroyVisibility(
                    programDefinition.id(), blockDefinition.id())
                .url();
        configureExistingPredicateUrl =
            routes.AdminProgramBlockPredicatesController.configureExistingVisibilityPredicate(
                    programDefinition.id(), blockDefinition.id())
                .url();
        configureNewPredicateUrl =
            routes.AdminProgramBlockPredicatesController.configureNewVisibilityPredicate(
                    programDefinition.id(), blockDefinition.id())
                .url();
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Predicate type %s is unsupported.", type));
    }
    InputTag csrfTag = makeCsrfTokenInputTag(request);

    String title =
        String.format("%s condition for %s", predicateTypeNameTitleCase, blockDefinition.name());
    String removePredicateFormId = String.format("form-%s", UUID.randomUUID());
    FormTag removePredicateForm =
        form(csrfTag)
            .withId(removePredicateFormId)
            .withMethod(POST)
            .withAction(removePredicateUrl)
            .with(
                submitButton(
                        String.format(
                            "Remove existing %s condition",
                            predicateTypeNameTitleCase.toLowerCase(Locale.ROOT)))
                    .withClasses(ButtonStyles.SOLID_BLUE)
                    .withForm(removePredicateFormId)
                    .withCondDisabled(!hasExistingPredicate));

    // Link back to the block editor.
    String editBlockUrl =
        routes.AdminProgramBlocksController.edit(programDefinition.id(), blockDefinition.id())
            .url();

    DivTag content =
        div()
            .withClasses("mx-6", "my-10", "flex", "flex-col", "gap-6")
            // Link back to the editor for this predicate's block.
            .with(
                new LinkElement()
                    .setHref(editBlockUrl)
                    .setIcon(Icons.ARROW_LEFT, IconPosition.START)
                    .setText(String.format("Back to edit %s", blockDefinition.name()))
                    .asAnchorText())
            .with(
                div()
                    .with(h1(title).withClasses("font-bold", "text-xl"))
                    .with(div(textNewCondition).withClasses("mb-2")))
            // Show the current predicate.
            .with(
                div()
                    .with(h2(h2CurrentCondition).withClasses("font-semibold", "text-lg"))
                    .with(existingPredicateDisplay.withClasses(ReferenceClasses.PREDICATE_DISPLAY)))
            .with(
                div(
                    redirectButton(
                            "edit-condition-button",
                            String.format(
                                "Edit existing %s condition",
                                predicateTypeNameTitleCase.toLowerCase(Locale.ROOT)),
                            configureExistingPredicateUrl)
                        .withCondDisabled(!hasExistingPredicate)
                        .withClasses(ButtonStyles.SOLID_BLUE)))
            // Show the control to remove the current predicate.
            .with(removePredicateForm)
            // Show all available questions that predicates can be made for, for this block.
            .with(
                div()
                    .with(h2("Questions").withClasses("font-semibold", "text-lg"))
                    .with(
                        predicateQuestions.isEmpty()
                            ? text(textNoAvailableQuestions)
                            : form(
                                    makeCsrfTokenInputTag(request),
                                    each(
                                        predicateQuestions,
                                        ProgramPredicatesEditViewV2
                                            ::renderPredicateQuestionCheckBoxRow),
                                    submitButton(
                                            hasExistingPredicate
                                                ? "Replace condition"
                                                : "Add condition")
                                        .withId("add-replace-predicate-condition")
                                        .withClasses(ButtonStyles.SOLID_BLUE))
                                .withAction(configureNewPredicateUrl)
                                .withMethod(POST)));

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                renderProgramInfoHeader(
                    programDefinition,
                    ImmutableList.of(ProgramHeaderButton.EDIT_PROGRAM_DETAILS),
                    request),
                content);

    Http.Flash flash = request.flash();
    if (flash.get("error").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.errorNonLocalized(flash.get("error").get()));
    } else if (flash.get("success").isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(flash.get("success").get()));
    }

    return layout.renderCentered(htmlBundle);
  }

  private static LabelTag renderPredicateQuestionCheckBoxRow(
      QuestionDefinition questionDefinition) {
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();

    InputTag checkbox =
        input()
            .withType("checkbox")
            .withClasses("mx-2", ReferenceClasses.PREDICATE_QUESTION_OPTIONS)
            .withName(String.format("question-%d", questionDefinition.getId()));

    return label()
        .withClasses("my-4", "p-4", "flex", "flex-row", "gap-4", "border", "border-gray-300")
        .with(checkbox)
        .with(
            div(Icons.questionTypeSvg(questionDefinition.getQuestionType())
                    .withClasses("shrink-0", "h-12", "w-6"))
                .withClasses("flex", "items-center"))
        .with(
            div()
                .withClasses("text-left")
                .with(
                    div(questionDefinition.getQuestionText().getDefault()).withClasses("font-bold"),
                    div(questionHelpText).withClasses("mt-1", "text-sm"),
                    div(String.format("Admin ID: %s", questionDefinition.getName()))
                        .withClasses("mt-1", "text-sm")));
  }

  @Override
  protected ProgramDisplayType getProgramDisplayStatus() {
    return DRAFT;
  }
}
