package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import services.applicant.question.ApplicantQuestion;
import services.question.types.RepeaterQuestionDefinition;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class RepeaterQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  public static final String REPEATER_FIELDS_ID = "repeater-fields";
  public static final String TEMPLATE_ID = "repeater-field-template";
  public static final String ADD_ELEMENT_BUTTON_ID = "repeater-field-add-button";
  public static final String REPEATER_FIELD_CLASS = "repeater-field";

  // TODO(#859): make this admin-configurable
  private static final String PLACEHOLDER = "Placeholder";

  private static final String REPEATER_FIELD_CLASSES =
      StyleUtils.joinStyles(REPEATER_FIELD_CLASS, Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4);

  private final ApplicantQuestion question;

  public RepeaterQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render() {
    ContainerTag repeaterFields = div().withId(REPEATER_FIELDS_ID);
    // TODO: add each answer as a repeaterField
    repeaterFields.with(repeaterField(PLACEHOLDER, Optional.empty(), 0));

    return div()
        .withClasses(Styles.MX_AUTO, Styles.W_MAX)
        .with(
            div()
                .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT)
                .withText(question.getQuestionText()),
            div()
                .withClasses(
                    ReferenceClasses.APPLICANT_QUESTION_HELP_TEXT,
                    Styles.TEXT_BASE,
                    Styles.FONT_THIN,
                    Styles.MB_2)
                .withText(question.getQuestionHelpText()),
            repeaterFields,
            button(ADD_ELEMENT_BUTTON_ID, "add element"))
        .withType("button");
  }

  public static Tag repeaterField(
      String placeholderText, Optional<String> existingOption, int index) {
    ContainerTag optionInput =
        FieldWithLabel.input()
            .setFieldName(RepeaterQuestionDefinition.REPEATED_ENTITY_NAME_KEY + "[]")
            .setPlaceholderText(placeholderText)
            .setValue(existingOption)
            .getContainer()
            .withClasses(Styles.FLEX, Styles.ML_2);
    Tag removeOptionBox =
        FieldWithLabel.checkbox()
            .setFieldName(RepeaterQuestionDefinition.REPEATED_ENTITY_NAME_KEY + "_delete[]")
            .setValue(String.valueOf(index))
            .getContainer();

    return div().withClasses(REPEATER_FIELD_CLASSES).with(optionInput, removeOptionBox);
  }
}
