package views.questiontypes;

import static j2html.TagCreator.div;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders an address question. */
public class AddressQuestionRenderer extends ApplicantQuestionRendererImpl {

  public AddressQuestionRenderer(ApplicantQuestion question) {
    super(question, InputFieldType.COMPOSITE);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.ADDRESS_QUESTION;
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds) {
    Messages messages = params.messages();
    AddressQuestion addressQuestion = question.createAddressQuestion();

    FieldWithLabel streetAddressField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getStreetPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STREET.getKeyName()))
            .setPlaceholderText(messages.at(MessageKey.ADDRESS_PLACEHOLDER_STREET.getKeyName()))
            .setValue(addressQuestion.getStreetValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getStreetPath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_STREET_1);

    FieldWithLabel addressOptionalField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getLine2Path().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_LINE_2.getKeyName()))
            .setPlaceholderText(messages.at(MessageKey.ADDRESS_PLACEHOLDER_LINE_2.getKeyName()))
            .setValue(addressQuestion.getLine2Value().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getLine2Path(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_STREET_2);

    FieldWithLabel cityField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getCityPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_CITY.getKeyName()))
            .setValue(addressQuestion.getCityValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getCityPath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_CITY);

    FieldWithLabel stateField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getStatePath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STATE.getKeyName()))
            .setValue(addressQuestion.getStateValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getStatePath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_STATE);

    FieldWithLabel zipField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getZipPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_ZIPCODE.getKeyName()))
            .setValue(addressQuestion.getZipValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getZipPath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_ZIP);

    if (!validationErrors.isEmpty()) {
      streetAddressField.forceAriaInvalid();
      cityField.forceAriaInvalid();
      stateField.forceAriaInvalid();
      zipField.forceAriaInvalid();
    }

    DivTag addressQuestionFormContent =
        div()
            .with(
                /** First line of address entry: Address line 1 AKA street address */
                streetAddressField.getInputTag(),
                /** Second line of address entry: Address line 2 AKA apartment, unit, etc. */
                addressOptionalField.getInputTag(),
                /** Third line of address entry: City, State, Zip */
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_3, Styles.GAP_2)
                    .with(
                        cityField.getInputTag(), stateField.getInputTag(), zipField.getInputTag()));

    return addressQuestionFormContent;
  }
}
