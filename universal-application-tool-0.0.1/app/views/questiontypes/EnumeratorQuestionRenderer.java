package views.questiontypes;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.Scalar;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class EnumeratorQuestionRenderer extends BaseHtmlView implements ApplicantQuestionRenderer {

  private static final String ENUMERATOR_FIELDS_ID = "enumerator-fields";
  private static final String ADD_ELEMENT_BUTTON_ID = "enumerator-field-add-button";
  private static final String ENUMERATOR_FIELD_TEMPLATE_ID = "enumerator-field-template";

  public static final String ENUMERATOR_FIELD_CLASSES =
      StyleUtils.joinStyles(
          ReferenceClasses.ENUMERATOR_FIELD, Styles.FLEX, Styles.FLEX_ROW, Styles.MB_4);

  private final ApplicantQuestion question;

  public EnumeratorQuestionRenderer(ApplicantQuestion question) {
    this.question = checkNotNull(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {

    Messages messages = params.messages();
    EnumeratorQuestion enumeratorQuestion = question.createEnumeratorQuestion();
    String localizedEntityType = enumeratorQuestion.getEntityType(messages);
    ImmutableList<String> entityNames = enumeratorQuestion.getEntityNames();

    ContainerTag enumeratorFields = div().withId(ENUMERATOR_FIELDS_ID);
    for (int index = 0; index < entityNames.size(); index++) {
      enumeratorFields.with(
          existingEnumeratorField(
              messages, Optional.of(entityNames.get(index)), index, localizedEntityType));
    }
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
            enumeratorFields,
            button(
                ADD_ELEMENT_BUTTON_ID,
                messages.at(
                    MessageKey.ENUMERATOR_BUTTON_ADD_ENTITY.getKeyName(), localizedEntityType)),
            fieldErrors(messages, enumeratorQuestion.getQuestionErrors()));
  }

  /**
   * Create an enumerator field for existing entries. These come with a checkbox to delete during
   * form submission.
   */
  private Tag existingEnumeratorField(
      Messages messages, Optional<String> existingOption, int index, String localizedEntityType) {
    ContainerTag entityNameInput =
        FieldWithLabel.input()
            .setFieldName(question.getContextualizedPath().toString())
            .setValue(existingOption)
            .getContainer()
            .withClasses(Styles.FLEX, Styles.ML_2);
    Tag removeEntityBox =
        FieldWithLabel.checkbox()
            .setFieldName(Path.empty().join(Scalar.DELETE_ENTITY).asArrayElement().toString())
            .setValue(String.valueOf(index))
            .getContainer()
            .attr(
                "aria-label",
                messages.at(
                    MessageKey.ENUMERATOR_BUTTON_ARIA_LABEL_DELETE_ENTITY.getKeyName(),
                    localizedEntityType));

    return div().withClasses(ENUMERATOR_FIELD_CLASSES).with(entityNameInput, removeEntityBox);
  }

  /**
   * Create an enumerator field template for new entries. These come with a button to delete itself.
   */
  public static Tag newEnumeratorFieldTemplate(
      Path contextualizedPath, String localizedEntityType, Messages messages) {
    ContainerTag entityNameInput =
        FieldWithLabel.input()
            .setFieldName(contextualizedPath.toString())
            .setPlaceholderText(
                messages.at(
                    MessageKey.ENUMERATOR_PLACEHOLDER_ENTITY_NAME.getKeyName(),
                    localizedEntityType))
            .getContainer()
            .withClasses(Styles.FLEX, Styles.ML_2);
    ContainerTag icon =
        Icons.svg(Icons.TRASH_CAN_SVG_PATH, 24)
            .withClasses(Styles.FLEX_SHRINK_0, Styles.H_12, Styles.W_6);
    Tag removeEntityButton =
        TagCreator.button(icon)
            .withType("button")
            .withClasses(Styles.FLEX, Styles.ML_4)
            .attr(
                "aria-label",
                messages.at(
                    MessageKey.ENUMERATOR_BUTTON_ARIA_LABEL_DELETE_ENTITY.getKeyName(),
                    localizedEntityType));
    return div()
        .withId(ENUMERATOR_FIELD_TEMPLATE_ID)
        .withClasses(StyleUtils.joinStyles(ENUMERATOR_FIELD_CLASSES, Styles.HIDDEN))
        .with(entityNameInput, removeEntityButton);
  }
}
