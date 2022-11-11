package views.questiontypes;

import static j2html.TagCreator.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import views.components.FieldWithLabel;
import views.components.SelectWithLabel;
import views.style.ReferenceClasses;

/** Renders an address question. */
public class AddressQuestionRenderer extends ApplicantCompositeQuestionRenderer {

  public AddressQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.ADDRESS_QUESTION;
  }

  @Override
  protected DivTag renderInputTags(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    Messages messages = params.messages();
    AddressQuestion addressQuestion = question.createAddressQuestion();

    FieldWithLabel streetAddressField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getStreetPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STREET.getKeyName()))
            .setPlaceholderText(messages.at(MessageKey.ADDRESS_PLACEHOLDER_STREET.getKeyName()))
            .setAutocomplete(Optional.of("address-line1"))
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
            .setAutocomplete(Optional.of("address-line2"))
            .setValue(addressQuestion.getLine2Value().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getLine2Path(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_STREET_2);

    FieldWithLabel cityField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getCityPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_CITY.getKeyName()))
            .setAutocomplete(Optional.of("address-level2"))
            .setValue(addressQuestion.getCityValue().orElse(""))
            .setFieldErrors(
                messages,
                validationErrors.getOrDefault(addressQuestion.getCityPath(), ImmutableSet.of()))
            .addReferenceClass(ReferenceClasses.ADDRESS_CITY);

    SelectWithLabel stateField =
        (SelectWithLabel)
            new SelectWithLabel()
                .setFieldName(addressQuestion.getStatePath().toString())
                .setValue(addressQuestion.getStateValue().orElse(""))
                .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STATE.getKeyName()))
                .setOptionGroups(
                    ImmutableList.of(
                        SelectWithLabel.OptionGroup.builder()
                            .setLabel(
                                messages.at(MessageKey.ADDRESS_LABEL_STATE_SELECT.getKeyName()))
                            .setOptions(stateOptions())
                            .build()))
                .setFieldErrors(
                    messages,
                    validationErrors.getOrDefault(
                        addressQuestion.getStatePath(), ImmutableSet.of()))
                .addReferenceClass(ReferenceClasses.ADDRESS_STATE);

    FieldWithLabel zipField =
        FieldWithLabel.input()
            .setFieldName(addressQuestion.getZipPath().toString())
            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_ZIPCODE.getKeyName()))
            .setAutocomplete(Optional.of("postal-code"))
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
                    .withClasses("grid", "grid-cols-3", "gap-3")
                    .with(
                        cityField.getInputTag(),
                        // TODO UI alignment issue fix tracked here at
                        // https://github.com/civiform/civiform/issues/3792
                        stateField.getSelectTag(),
                        zipField.getInputTag()));

    return addressQuestionFormContent;
  }

  /** Provides a list of State options as mentioned in https://pe.usps.com/text/pub28/28apb.htm */
  private static ImmutableList<SelectWithLabel.OptionValue> stateOptions() {

    return STATE_ABBREVIATIONS.stream();

      SelectWithLabel.OptionValue.builder().setLabel("AL").setValue("AL").build();

        ImmutableList<String> STATE_ABBREVIATIONS = ImmutableList.of(
        "AL",
        "AK",
        "AS",
        "AZ",
        "AR",
        "CA",
        "CO",
        "CT",
        "DE",
        "DC",
        "FM",
        "FL",
        "GA",
        "GU",
        "HI",
        "ID",
        "IL",
        "IN",
        "IA",
        "KS",
        "KY",
        "LA",
        "ME",
        "MH",
        "MD",
        "MA",
        "MI",
        "MN",
        "MS",
        "MO",
        "MT",
        "NE",
        "NV",
        "NH",
        "NJ",
        "NM",
        "NY",
        "NC",
        "ND",
        "MP",
        "OH",
        "OK",
        "OR",
        "PW",
        "PA",
        "PR",
        "RI",
        "SC",
        "SD",
        "TN",
        "TX",
        "UT",
        "VT",
        "VI",
        "VA",
        "WA",
        "WV",
        "WI",
        "WY");
  }
}
