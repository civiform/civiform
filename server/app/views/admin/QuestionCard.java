package views.admin;

import static j2html.TagCreator.div;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.UlTag;
import java.util.Optional;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
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
   * Renders a question for import, including info about the question and possibly its
   * duplicate-handling options
   *
   * @param questionDefinition the question definition to render
   * @param badgeForImport a badge indicating if the question is new or a duplicate
   * @param maybeDuplicateHandlingForImport a div tag containing the duplicate handling options, if
   *     this question is a duplicate
   * @return a div tag containing the rendered question
   */
  public static DivTag renderForImport(
      QuestionDefinition questionDefinition,
      DivTag badgeForImport,
      Optional<FieldsetTag> maybeDuplicateHandlingForImport) {
    return render(
        questionDefinition,
        /* malformedQuestionDefinition= */ false,
        /* editButtonsForProgramPage= */ ImmutableList.of(),
        Optional.of(badgeForImport),
        maybeDuplicateHandlingForImport,
        /* visibilityConditionEditLinks= */ Optional.empty());
  }

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
        /* maybeBadgeForImport= */ Optional.empty(),
        /* maybeDuplicateHandlingForImport= */ Optional.empty(),
        visibilityConditionEditLinks);
  }

  /**
   * Renders an individual question, including the description and any toggles or tags that should
   * be shown next to the question in the list of questions.
   */
  private static DivTag render(
      QuestionDefinition questionDefinition,
      boolean malformedQuestionDefinition,
      ImmutableList<DomContent> editButtonsForProgramPage,
      Optional<DivTag> maybeBadgeForImport,
      Optional<FieldsetTag> maybeDuplicateHandlingForImport,
      Optional<DivTag> visibilityConditionEditLinks) {
    boolean showOneOrMoreBadges =
        (!malformedQuestionDefinition && questionDefinition.isUniversal())
            || maybeBadgeForImport.isPresent();
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
                showOneOrMoreBadges,
                div()
                    .withClasses("flex", "mx-4", "mt-4", "mb-2")
                    .condWith(
                        !malformedQuestionDefinition && questionDefinition.isUniversal(),
                        ViewUtils.makeUniversalBadge(questionDefinition, "mr-2"))
                    // Default to null, since condWith doesn't short-circuit
                    .condWith(maybeBadgeForImport.isPresent(), maybeBadgeForImport.orElse(null)));
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
                div()
                    .with(TextFormatter.formatTextForAdmins(questionHelpText))
                    .withClasses("mt-1", "text-sm"),
                p(String.format("Admin ID: %s", questionDefinition.getName()))
                    .withClasses("mt-1", "text-sm"),

                // Only show multi-option text during program import
                maybeBadgeForImport.isPresent()
                        && questionDefinition.getQuestionType().isMultiOptionType()
                    ? getOptions((MultiOptionQuestionDefinition) questionDefinition)
                    : null);

    DivTag row =
        div()
            .withClasses("flex", "gap-4", "items-center", "px-4", "py-2")
            .with(icon, content)
            .condWith(
                maybeDuplicateHandlingForImport.isPresent(),
                // Default to null, since condWith doesn't short-circuit
                maybeDuplicateHandlingForImport.orElse(null))
            .with(editButtonsForProgramPage);

    return cardDiv
        .with(row)
        .condWith(
            visibilityConditionEditLinks.isPresent(), visibilityConditionEditLinks.orElse(null));
  }

  private static UlTag getOptions(MultiOptionQuestionDefinition question) {
    UlTag options = ul().withClasses("list-disc", "mx-4", "mt-2");
    for (QuestionOption option : question.getOptions()) {
      options.with(li(option.optionText().getDefault()));
    }
    return options;
  }
}
