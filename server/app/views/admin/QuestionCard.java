package views.admin;

import static j2html.TagCreator.div;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.ViewUtils;
import views.components.Icons;
import views.components.SvgTag;
import views.components.TextFormatter;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Contains methods for rendering questions across admin pages. */
public final class QuestionCard {
  /**
   * Renders a question for a program, including info about the question and possibly some editing
   * controls
   *
   * @param questionDefinition the question definition to render
   * @param malformedQuestionDefinition whether there is an issue with the definition
   * @param editButtonsForProgramPage the div tags containing buttons/icons showing the options to
   *     edit the question
   * @param visibilityConditionEditLinks the optional div tag containing edit links if this question
   *     is used in visibility conditions.
   * @return a div tag containing the rendered question
   */
  public static DivTag renderForProgramPage(
      QuestionDefinition questionDefinition,
      boolean malformedQuestionDefinition,
      ImmutableList<DomContent> editButtonsForProgramPage,
      Optional<DivTag> visibilityConditionEditLinks) {
    return render(
        questionDefinition,
        malformedQuestionDefinition,
        editButtonsForProgramPage,
        visibilityConditionEditLinks,
        /* includeHelpText= */ true,
        /* includeUniversalBadge= */ true);
  }

  /**
   * Renders a stripped-down card for use as the chosen initial question on the enumerator-creation
   * form. Hides the help text, optional toggle, move arrows, address-correction toggle, edit link,
   * and visibility accordion — none of those apply while the initial question has not yet been
   * attached to the block.
   *
   * @param deleteButton optional HTMX-driven Delete button to swap the slot back to its empty
   *     state.
   */
  public static DivTag renderForInitialQuestion(
      QuestionDefinition selectedQuestion, Optional<ButtonTag> deleteButton) {
    return render(
        selectedQuestion,
        /* malformedQuestionDefinition= */ selectedQuestion instanceof NullQuestionDefinition,
        /* editButtonsForProgramPage= */ deleteButton.stream()
            .collect(ImmutableList.toImmutableList()),
        /* visibilityConditionEditLinks= */ Optional.empty(),
        /* includeHelpText= */ false,
        /* includeUniversalBadge= */ false);
  }

  /**
   * Renders an individual question, including the description and any toggles or tags that should
   * be shown next to the question in the list of questions.
   */
  private static DivTag render(
      QuestionDefinition questionDefinition,
      boolean malformedQuestionDefinition,
      ImmutableList<DomContent> editButtonsForProgramPage,
      Optional<DivTag> visibilityConditionEditLinks,
      boolean includeHelpText,
      boolean includeUniversalBadge) {
    boolean showUniversalBadge =
        includeUniversalBadge && !malformedQuestionDefinition && questionDefinition.isUniversal();
    DivTag cardDiv =
        div()
            .withData("testid", "question-admin-name-" + questionDefinition.getName())
            .withClasses(
                ReferenceClasses.PROGRAM_QUESTION,
                "my-2",
                iffElse(malformedQuestionDefinition, "border-2", "border"),
                iffElse(malformedQuestionDefinition, "border-red-500", "border-gray-200"),
                "items-center",
                "rounded-md",
                StyleUtils.hover("text-gray-800", "bg-gray-100"))
            .condWith(
                showUniversalBadge,
                div()
                    .withClasses("flex", "mx-4", "mt-4", "mb-2")
                    .with(ViewUtils.makeUniversalBadge(questionDefinition, "mr-2")));
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
                iff(
                    malformedQuestionDefinition,
                    p("This is not pointing at the latest version")
                        .withClasses("text-red-500", "font-bold")),
                iff(
                    malformedQuestionDefinition,
                    p("Edit the program and try republishing").withClass("text-red-500")),
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(
                            questionDefinition.getQuestionText().getDefault()))
                    .withData("testid", "question-div"),
                iff(
                    includeHelpText,
                    div()
                        .with(TextFormatter.formatTextForAdmins(questionHelpText))
                        .withClasses("mt-1", "text-sm")),
                p(String.format("Admin ID: %s", questionDefinition.getName()))
                    .withClasses("mt-1", "text-sm"));

    DivTag row =
        div()
            .withClasses("flex", "gap-4", "items-center", "px-4", "py-2")
            .with(icon, content)
            .with(editButtonsForProgramPage);

    return cardDiv
        .with(row)
        .condWith(
            visibilityConditionEditLinks.isPresent(), visibilityConditionEditLinks.orElse(null));
  }
}
