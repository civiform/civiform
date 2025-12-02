package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import play.mvc.Http;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.style.StyleUtils;

/**
 * Contains a helper method to render the create question button with it's corresponding dropdown.
 */
public final class CreateQuestionButton {

  /** Renders the "Create new question" button with a dropdown for each question type. */
  public static DivTag renderCreateQuestionButton(
      String questionCreateRedirectUrl,
      boolean isPrimaryButton,
      SettingsManifest settingsManifest,
      Http.Request request) {
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
                // Small padding at the abottom for visual spacing
                "pb-3",
                "display-none");

    for (QuestionType type : QuestionType.values()) {
      // Do not attempt to render a null question
      if (type == QuestionType.NULL_QUESTION) {
        continue;
      }
      if (type == QuestionType.YES_NO && !settingsManifest.getYesNoQuestionEnabled()) {
        continue;
      }
      if (type == QuestionType.MAP && !settingsManifest.getMapQuestionEnabled(request)) {
        continue;
      }

      String typeString = type.toString().toLowerCase(Locale.ROOT);
      String link =
          controllers.admin.routes.AdminQuestionController.newOne(
                  typeString, questionCreateRedirectUrl)
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
