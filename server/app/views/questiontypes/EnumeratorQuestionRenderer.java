package views.questiontypes;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.tags.EmptyTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.Scalar;
import views.components.FieldWithLabel;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders an enumerator question. */
public final class EnumeratorQuestionRenderer extends ApplicantCompositeQuestionRenderer {

  // Hardcoded ids used by client side javascript.
  private static final String ENUMERATOR_FIELDS_ID = "enumerator-fields";
  private static final String ADD_ELEMENT_BUTTON_ID = "enumerator-field-add-button";
  private static final String ENUMERATOR_FIELD_TEMPLATE_ID = "enumerator-field-template";
  private static final String ENUMERATOR_FIELD_TEMPLATE_INPUT_ID =
      "enumerator-field-template-input";
  private static final String DELETE_ENTITY_TEMPLATE_ID = "enumerator-delete-template";

  private static final String ENUMERATOR_FIELD_CLASSES =
      StyleUtils.joinStyles(
          ReferenceClasses.ENUMERATOR_FIELD, "grid", "grid-cols-2", "gap-4", "mb-4");

  public EnumeratorQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.ENUMERATOR_QUESTION;
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    Messages messages = params.messages();
    EnumeratorQuestion enumeratorQuestion = question.createEnumeratorQuestion();
    String localizedEntityType = enumeratorQuestion.getEntityType();
    ImmutableList<String> entityNames = enumeratorQuestion.getEntityNames();
    boolean hasErrors = !validationErrors.isEmpty();

    DivTag enumeratorFields = div().withId(ENUMERATOR_FIELDS_ID);
    for (int index = 0; index < entityNames.size(); index++) {
      enumeratorFields.with(
          enumeratorField(
              messages,
              localizedEntityType,
              question.getContextualizedPath(),
              /* existingEntity= */ Optional.of(entityNames.get(index)),
              /* existingIndex= */ Optional.of(index),
              /* extraStyle= */ Optional.empty(),
              /* isDisabled= */ false,
              hasErrors,
              /* elementId= */ Optional.empty()));
    }

    DivTag enumeratorQuestionFormContent =
        div()
            .with(hiddenDeleteInputTemplate())
            .with(enumeratorFields)
            .with(
                button()
                    .withId(ADD_ELEMENT_BUTTON_ID)
                    // need to specify type "button" to avoid default onClick browser behavior
                    .withType("button")
                    .condAttr(hasErrors, "aria-invalid", "true")
                    .withClasses(
                        ApplicantStyles.BUTTON_ENUMERATOR_ADD_ENTITY,
                        StyleUtils.disabled("bg-gray-200", "text-gray-400"))
                    .with(
                        span("＋ ").attr("aria-hidden", "true"),
                        span(
                            messages.at(
                                MessageKey.ENUMERATOR_BUTTON_ADD_ENTITY.getKeyName(),
                                localizedEntityType))))
            .with(
                // Add the hidden enumerator field template.
                enumeratorField(
                        messages,
                        localizedEntityType,
                        question.getContextualizedPath(),
                        /* existingEntity= */ Optional.empty(),
                        /* existingIndex= */ Optional.empty(),
                        /* extraStyle= */ Optional.of("hidden"),
                        // Do not submit this with the form.
                        /* isDisabled= */ true,
                        hasErrors,
                        /* elementId= */ Optional.of(ENUMERATOR_FIELD_TEMPLATE_INPUT_ID))
                    .withId(ENUMERATOR_FIELD_TEMPLATE_ID))
            .withData(
                "label-text",
                messages.at(
                    MessageKey.ENUMERATOR_PLACEHOLDER_ENTITY_NAME.getKeyName(),
                    localizedEntityType))
            .withData(
                "button-text",
                messages.at(
                    MessageKey.ENUMERATOR_BUTTON_REMOVE_ENTITY.getKeyName(), localizedEntityType));

    return enumeratorQuestionFormContent;
  }

  /**
   * Create an enumerator field for existing entries. These come with a button to delete during form
   * submission.
   */
  private static DivTag enumeratorField(
      Messages messages,
      String localizedEntityType,
      Path contextualizedPath,
      Optional<String> existingEntity,
      Optional<Integer> existingIndex,
      Optional<String> extraStyle,
      boolean isDisabled,
      boolean hasErrors,
      Optional<String> elementId) {

    String indexString = "";
    if (existingIndex.isPresent()) {
      indexString = " #" + String.valueOf(existingIndex.get() + 1);
    }
    FieldWithLabel entityNameInputField =
        FieldWithLabel.input()
            .setFieldName(contextualizedPath.toString())
            .setValue(existingEntity)
            .setDisabled(isDisabled)
            .setLabelText(
                messages.at(
                        MessageKey.ENUMERATOR_PLACEHOLDER_ENTITY_NAME.getKeyName(),
                        localizedEntityType)
                    + indexString)
            .addReferenceClass(ReferenceClasses.ENTITY_NAME_INPUT);
    if (elementId.isPresent()) {
      entityNameInputField.setId(elementId.get());
    }
    if (hasErrors) {
      entityNameInputField.forceAriaInvalid();
    }
    String confirmationMessage =
        messages.at(MessageKey.ENUMERATOR_DIALOG_CONFIRM_DELETE.getKeyName(), localizedEntityType);
    ButtonTag removeEntityButton =
        TagCreator.button()
            .withType("button")
            .withCondId(existingIndex.isPresent(), existingIndex.map(String::valueOf).orElse(""))
            .attr(
                "onclick",
                String.format(
                    "if(confirm('%s')){ return true; } else { var e = arguments[0] ||"
                        + " window.event; e.stopImmediatePropagation(); return false; }",
                    confirmationMessage))
            .withClasses(
                ReferenceClasses.ENUMERATOR_EXISTING_DELETE_BUTTON,
                ApplicantStyles.BUTTON_ENUMERATOR_REMOVE_ENTITY)
            .withText(
                messages.at(
                        MessageKey.ENUMERATOR_BUTTON_REMOVE_ENTITY.getKeyName(),
                        localizedEntityType)
                    + indexString);

    return div()
        .withClasses(StyleUtils.joinStyles(ENUMERATOR_FIELD_CLASSES, extraStyle.orElse("")))
        .with(entityNameInputField.getInputTag(), removeEntityButton);
  }

  /**
   * A hidden template to copy when deleting an existing entity. This will allow us to mark existing
   * entities for deletion.
   */
  private static EmptyTag hiddenDeleteInputTemplate() {
    return input()
        .withId(DELETE_ENTITY_TEMPLATE_ID)
        .withName(Path.empty().join(Scalar.DELETE_ENTITY).asArrayElement().toString())
        .isDisabled() // do not submit this with the form
        .withClasses("hidden");
  }
}
