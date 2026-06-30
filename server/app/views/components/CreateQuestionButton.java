package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import play.mvc.Http;
import services.ProgramBlockValidation;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.style.StyleUtils;

/**
 * Contains helper methods to render the create question button with it's corresponding dropdown.
 */
public final class CreateQuestionButton {

  /**
   * Renders the "Create new question" button with a question type dropdown, customized for the
   * standalone admin questions list page.
   */
  public static DivTag renderCreateQuestionButtonForQuestionListPage(
      SettingsManifest settingsManifest, Http.Request request) {
    return renderCreateQuestionButton(
        controllers.admin.routes.AdminQuestionController.index(Optional.empty()).url(),
        /* isPrimaryButton= */ true,
        /* enumeratorQuestion= */ Optional.empty(),
        /* isRepeatingBlock= */ true,
        settingsManifest,
        request,
        /* isEmptyBlock= */ true,
        /* isQuestionPage= */ true,
        /* isInitialQuestion= */ false);
  }

  /**
   * Renders the "Create new question" button with a question type dropdown, customized for the
   * program block edit page's question bank sidebar.
   */
  public static DivTag renderCreateQuestionButtonForProgramQuestionBank(
      String questionCreateRedirectUrl,
      Optional<String> enumeratorQuestion,
      boolean isRepeatingBlock,
      SettingsManifest settingsManifest,
      Http.Request request,
      boolean isEmptyBlock,
      boolean isInitialQuestion) {
    return renderCreateQuestionButton(
        questionCreateRedirectUrl,
        /* isPrimaryButton= */ false,
        enumeratorQuestion,
        isRepeatingBlock,
        settingsManifest,
        request,
        isEmptyBlock,
        /* isQuestionPage= */ false,
        isInitialQuestion);
  }

  /**
   * Renders the "Create new question" button with a question type dropdown. This method should be
   * the only way the button is rendered to keep all uses consistent.
   */
  private static DivTag renderCreateQuestionButton(
      String questionCreateRedirectUrl,
      boolean isPrimaryButton,
      Optional<String> enumeratorQuestion,
      boolean isRepeatingBlock,
      SettingsManifest settingsManifest,
      Http.Request request,
      boolean isEmptyBlock,
      boolean isQuestionPage,
      boolean isInitialQuestion) {
    String parentId = "create-question-button";
    String dropdownId = parentId + "-dropdown";
    ButtonTag createNewQuestionButton =
        button("Create new question")
            .withId(parentId)
            .withType("button")
            .withClass(
                isPrimaryButton ? ButtonStyles.SOLID_BLUE : ButtonStyles.OUTLINED_WHITE_WITH_ICON);
    DivTag dropdown =
        div()
            .withId(dropdownId)
            .withData("testId", dropdownId)
            .withClasses(
                "z-50",
                "border",
                "bg-white",
                "text-gray-600",
                "shadow-lg",
                "absolute",
                "ml-3",
                "mt-1",
                // Small padding at the bottom for visual spacing
                "pb-3",
                "hidden");

    for (QuestionType type : QuestionType.values()) {
      // Do not attempt to render a null question
      if (type == QuestionType.NULL_QUESTION) {
        continue;
      }
      if (type == QuestionType.YES_NO && !settingsManifest.getYesNoQuestionEnabled()) {
        continue;
      }
      // Only filter Enumerator on program block pages, not on the standalone questions list page
      if (type == QuestionType.ENUMERATOR
          && !isQuestionPage
          && (settingsManifest.getEnumeratorImprovementsEnabled(request) || !isEmptyBlock)) {
        continue;
      }
      if (isInitialQuestion
          && !ProgramBlockValidation.VALID_INITIAL_QUESTION_TYPES.contains(type)) {
        continue;
      }

      String typeString = type.toString().toLowerCase(Locale.ROOT);
      String link =
          controllers.admin.routes.AdminQuestionController.newOne(
                  typeString,
                  questionCreateRedirectUrl,
                  enumeratorQuestion,
                  Optional.of(Boolean.toString(isRepeatingBlock)))
              .url();
      ATag linkTag =
          a().withHref(link)
              .withId(String.format("create-%s-question", typeString))
              .withClasses(
                  "block",
                  "p-3",
                  "bg-white",
                  "text-gray-600",
                  StyleUtils.hover("bg-gray-100", "text-gray-800"))
              .with(
                  Icons.questionTypeSvgWithId(type)
                      .withClasses("inline-block", "h-6", "w-6", "mr-1", "text-sm"))
              .with(p(type.getLabel()).withClasses("ml-2", "mr-4", "inline", "text-sm"));
      dropdown.with(linkTag);
    }
    return div().withClasses("relative").with(createNewQuestionButton, dropdown);
  }
}
