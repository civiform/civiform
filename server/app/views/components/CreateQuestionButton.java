package views.components;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.p;

import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import services.question.types.QuestionType;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Contains a helper method to render the create question button with it's corresponding dropdown.
 */
public final class CreateQuestionButton {
  /** Renders the "Create new question" button with a dropdown for each question type. */
  public static DivTag renderCreateQuestionButton(String questionCreateRedirectUrl) {
    String parentId = "create-question-button";
    String dropdownId = parentId + "-dropdown";
    ButtonTag createNewQuestionButton =
        button("Create new question")
            .withId(parentId)
            .withType("button")
            .withClass(AdminStyles.PRIMARY_BUTTON_STYLES);
    DivTag dropdown =
        div()
            .withId(dropdownId)
            .withClasses(
                Styles.Z_50,
                Styles.BORDER,
                Styles.BG_WHITE,
                Styles.TEXT_GRAY_600,
                Styles.SHADOW_LG,
                Styles.ABSOLUTE,
                Styles.ML_3,
                Styles.MT_1,
                Styles.HIDDEN);

    for (QuestionType type : QuestionType.values()) {
      String typeString = type.toString().toLowerCase();
      String link =
          controllers.admin.routes.AdminQuestionController.newOne(
                  typeString, questionCreateRedirectUrl)
              .url();
      ATag linkTag =
          a().withHref(link)
              .withId(String.format("create-%s-question", typeString))
              .withClasses(
                  Styles.BLOCK,
                  Styles.P_3,
                  Styles.BG_WHITE,
                  Styles.TEXT_GRAY_600,
                  StyleUtils.hover(Styles.BG_GRAY_100, Styles.TEXT_GRAY_800))
              .with(
                  Icons.questionTypeSvg(type)
                      .withClasses(
                          Styles.INLINE_BLOCK, Styles.H_6, Styles.W_6, Styles.MR_1, Styles.TEXT_SM))
              .with(
                  p(type.getLabel())
                      .withClasses(
                          Styles.ML_2,
                          Styles.MR_4,
                          Styles.INLINE,
                          Styles.TEXT_SM,
                          Styles.UPPERCASE));
      dropdown.with(linkTag);
    }
    return div().withClasses(Styles.RELATIVE).with(createNewQuestionButton, dropdown);
  }
}
