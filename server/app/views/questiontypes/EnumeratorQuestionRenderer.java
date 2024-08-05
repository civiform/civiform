package views.questiontypes;

import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.TagCreator;
import j2html.attributes.Attr;
import j2html.tags.EmptyTag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import java.util.OptionalInt;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EnumeratorQuestion;
import services.applicant.question.Scalar;
import views.ViewUtils;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
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
          ReferenceClasses.ENUMERATOR_FIELD,
          "grid",
          "grid-cols-1",
          "sm:grid-cols-2",
          "gap-4",
          "mb-4");

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
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      boolean isOptional) {
    Messages messages = params.messages();
    EnumeratorQuestion enumeratorQuestion = applicantQuestion.createEnumeratorQuestion();
    String localizedEntityType = enumeratorQuestion.getEntityType();
    ImmutableList<String> entityNames = enumeratorQuestion.getEntityNames();
    OptionalInt minEntities = enumeratorQuestion.getMinEntities();
    OptionalInt maxEntities = enumeratorQuestion.getMaxEntities();
    boolean hasErrors = !validationErrors.isEmpty();

    DivTag enumeratorFields = div().withId(ENUMERATOR_FIELDS_ID);
    for (int index = 0; index < entityNames.size(); index++) {
      enumeratorFields.with(
          enumeratorField(
              messages,
              localizedEntityType,
              applicantQuestion.getContextualizedPath(),
              /* existingEntity= */ Optional.of(entityNames.get(index)),
              /* existingIndex= */ Optional.of(index),
              /* extraStyle= */ Optional.empty(),
              /* isDisabled= */ false,
              hasErrors,
              isOptional,
              /* elementId= */ Optional.empty()));
    }

    DivTag enumeratorQuestionFormContent =
        div()
            .with(hiddenDeleteInputTemplate())
            .with(enumeratorFields)
            .with(
                ViewUtils.makeSvgTextButton(
                        messages.at(
                            MessageKey.ENUMERATOR_BUTTON_ADD_ENTITY.getKeyName(),
                            localizedEntityType),
                        Icons.PLUS)
                    .withId(ADD_ELEMENT_BUTTON_ID)
                    // need to specify type "button" to avoid default onClick browser behavior
                    .withType("button")
                    .condAttr(params.autofocusSingleField(), Attr.AUTOFOCUS, "")
                    .condAttr(hasErrors, "aria-invalid", "true")
                    .withData(
                        "min-entities",
                        minEntities.isPresent() ? String.valueOf(minEntities.getAsInt()) : "")
                    .withData(
                        "max-entities",
                        maxEntities.isPresent() ? String.valueOf(maxEntities.getAsInt()) : "")
                    .withClasses(
                        ButtonStyles.SOLID_BLUE_WITH_ICON,
                        "normal-case",
                        "font-normal",
                        "px-4",
                        StyleUtils.disabled("bg-gray-200", "text-gray-400")))
            .with(
                // Add the hidden enumerator field template.
                enumeratorField(
                        messages,
                        localizedEntityType,
                        applicantQuestion.getContextualizedPath(),
                        /* existingEntity= */ Optional.empty(),
                        /* existingIndex= */ Optional.empty(),
                        /* extraStyle= */ Optional.of("hidden"),
                        // Do not submit this with the form.
                        /* isDisabled= */ true,
                        hasErrors,
                        isOptional,
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
      boolean isOptional,
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
            .setAriaRequired(!isOptional)
            .setLabelText(
                messages.at(
                        MessageKey.ENUMERATOR_PLACEHOLDER_ENTITY_NAME.getKeyName(),
                        localizedEntityType)
                    + indexString)
            .addReferenceClass(ReferenceClasses.ENTITY_NAME_INPUT);
    if (!isDisabled) {
      entityNameInputField.setAttribute("data-entity-input");
    }
    if (elementId.isPresent()) {
      entityNameInputField.setId(elementId.get());
    }
    if (hasErrors) {
      entityNameInputField.forceAriaInvalid();
      entityNameInputField.focusOnError();
    }
    String confirmationMessage =
        messages.at(
            MessageKey.ENUMERATOR_DIALOG_CONFIRM_DELETE_ALL_BUTTONS_SAVE.getKeyName(),
            localizedEntityType);
    ButtonTag removeEntityButton =
        TagCreator.button()
            .withType("button")
            .withCondId(existingIndex.isPresent(), existingIndex.map(String::valueOf).orElse(""))
            .withData("confirmation-message", confirmationMessage)
            .withClasses(
                ReferenceClasses.ENUMERATOR_EXISTING_DELETE_BUTTON,
                StyleUtils.removeStyles(ButtonStyles.OUTLINED_TRANSPARENT, "px-8"),
                "text-base",
                "normal-case",
                "font-normal",
                "justify-self-end",
                "self-center")
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
