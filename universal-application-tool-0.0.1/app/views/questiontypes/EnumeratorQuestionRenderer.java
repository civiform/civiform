package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
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
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class EnumeratorQuestionRenderer extends ApplicantQuestionRenderer {

  private static final String ENUMERATOR_FIELDS_ID = "enumerator-fields";
  private static final String ADD_ELEMENT_BUTTON_ID = "enumerator-field-add-button";
  private static final String ENUMERATOR_FIELD_TEMPLATE_ID = "enumerator-field-template";
  private static final String DELETE_ENTITY_TEMPLATE_ID = "enumerator-delete-template";

  public static final String ENUMERATOR_FIELD_CLASSES =
      StyleUtils.joinStyles(
          ReferenceClasses.ENUMERATOR_FIELD,
          Styles.GRID,
          Styles.GRID_COLS_2,
          Styles.GAP_4,
          Styles.MB_4);

  public EnumeratorQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    EnumeratorQuestion enumeratorQuestion = question.createEnumeratorQuestion();
    String localizedEntityType = enumeratorQuestion.getEntityType();
    ImmutableList<String> entityNames = enumeratorQuestion.getEntityNames();

    ContainerTag enumeratorFields = div().withId(ENUMERATOR_FIELDS_ID);
    for (int index = 0; index < entityNames.size(); index++) {
      enumeratorFields.with(
          enumeratorField(
              messages,
              localizedEntityType,
              question.getContextualizedPath(),
              Optional.of(entityNames.get(index)),
              Optional.of(index)));
    }

    Tag enumeratorQuestionFormContent =
        div()
            .with(hiddenDeleteInputTemplate())
            .with(enumeratorFields)
            .with(
                BaseHtmlView.button(
                        ADD_ELEMENT_BUTTON_ID,
                        String.format(
                            "＋ %s",
                            messages.at(
                                MessageKey.ENUMERATOR_BUTTON_ADD_ENTITY.getKeyName(),
                                localizedEntityType)))
                    .withClasses(ApplicantStyles.BUTTON_ENUMERATOR_ADD_ENTITY));

    return renderInternal(messages, enumeratorQuestionFormContent);
  }

  /**
   * Create an enumerator field for existing entries. These come with a checkbox to delete during
   * form submission.
   */
  private static Tag enumeratorField(
      Messages messages,
      String localizedEntityType,
      Path contextualizedPath,
      Optional<String> existingEntity,
      Optional<Integer> existingIndex) {

    ContainerTag entityNameInput =
        FieldWithLabel.input()
            .setFieldName(contextualizedPath.toString())
            .setValue(existingEntity)
            .setPlaceholderText(
                messages.at(
                    MessageKey.ENUMERATOR_PLACEHOLDER_ENTITY_NAME.getKeyName(),
                    localizedEntityType))
            .addReferenceClass(ReferenceClasses.ENTITY_NAME_INPUT)
            .getContainer();
    Tag removeEntityButton =
        TagCreator.button()
            .withType("button")
            .withCondId(existingIndex.isPresent(), existingIndex.map(String::valueOf).orElse(""))
            .withClasses(
                existingEntity.isPresent()
                    ? StyleUtils.joinStyles(ReferenceClasses.ENUMERATOR_EXISTING_DELETE_BUTTON)
                    : "",
                ApplicantStyles.BUTTON_ENUMERATOR_REMOVE_ENTITY)
            .withText(
                messages.at(
                    MessageKey.ENUMERATOR_BUTTON_REMOVE_ENTITY.getKeyName(), localizedEntityType));

    return div().withClasses(ENUMERATOR_FIELD_CLASSES).with(entityNameInput, removeEntityButton);
  }

  /**
   * Create an enumerator field template for new entries. These come with a button to delete itself.
   */
  public static Tag newEnumeratorFieldTemplate(
      Path contextualizedPath, String localizedEntityType, Messages messages) {
    return enumeratorField(
            messages, localizedEntityType, contextualizedPath, Optional.empty(), Optional.empty())
        .withId(ENUMERATOR_FIELD_TEMPLATE_ID)
        .withClasses(StyleUtils.joinStyles(ENUMERATOR_FIELD_CLASSES, Styles.HIDDEN));
  }

  /**
   * A hidden template to copy when deleting an existing entity. This will allow us to mark existing
   * entities for deletion.
   */
  private static EmptyTag hiddenDeleteInputTemplate() {
    return input()
        .withId(DELETE_ENTITY_TEMPLATE_ID)
        .withName(Path.empty().join(Scalar.DELETE_ENTITY).asArrayElement().toString())
        .attr(Attr.DISABLED, true) // do not submit this with the form
        .withClasses(Styles.HIDDEN);
  }
}
