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
                .addSelectClass("py-6")
                .addSelectClass("h-5")
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

    // stateField.getSelectTag().withClasses("py-3");

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
                        stateField.getSelectTag(),
                        zipField.getInputTag()));

    return addressQuestionFormContent;
  }

  /** Provides a list of State options as mentioned in https://pe.usps.com/text/pub28/28apb.htm */
  private static ImmutableList<SelectWithLabel.OptionValue> stateOptions() {
    return ImmutableList.of(
        SelectWithLabel.OptionValue.builder().setLabel("AL").setValue("AL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("AK").setValue("AK").build(),
        SelectWithLabel.OptionValue.builder().setLabel("AS").setValue("AS").build(),
        SelectWithLabel.OptionValue.builder().setLabel("AZ").setValue("AZ").build(),
        SelectWithLabel.OptionValue.builder().setLabel("AR").setValue("AR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("CA").setValue("CA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("CO").setValue("CO").build(),
        SelectWithLabel.OptionValue.builder().setLabel("CT").setValue("CT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("DE").setValue("DE").build(),
        SelectWithLabel.OptionValue.builder().setLabel("DC").setValue("DC").build(),
        SelectWithLabel.OptionValue.builder().setLabel("FM").setValue("FM").build(),
        SelectWithLabel.OptionValue.builder().setLabel("FL").setValue("FL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("GA").setValue("GA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("GU").setValue("GU").build(),
        SelectWithLabel.OptionValue.builder().setLabel("HI").setValue("HI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("ID").setValue("ID").build(),
        SelectWithLabel.OptionValue.builder().setLabel("IL").setValue("IL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("IN").setValue("IN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("IA").setValue("IA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("KS").setValue("KS").build(),
        SelectWithLabel.OptionValue.builder().setLabel("KY").setValue("KY").build(),
        SelectWithLabel.OptionValue.builder().setLabel("LA").setValue("LA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("ME").setValue("ME").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MH").setValue("MH").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MD").setValue("MD").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MA").setValue("MA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MI").setValue("MI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MN").setValue("MN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MS").setValue("MS").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MO").setValue("MO").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MT").setValue("MT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NE").setValue("NE").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NV").setValue("NV").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NH").setValue("NH").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NJ").setValue("NJ").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NM").setValue("NM").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NY").setValue("NY").build(),
        SelectWithLabel.OptionValue.builder().setLabel("NC").setValue("NC").build(),
        SelectWithLabel.OptionValue.builder().setLabel("ND").setValue("ND").build(),
        SelectWithLabel.OptionValue.builder().setLabel("MP").setValue("MP").build(),
        SelectWithLabel.OptionValue.builder().setLabel("OH").setValue("OH").build(),
        SelectWithLabel.OptionValue.builder().setLabel("OK").setValue("OK").build(),
        SelectWithLabel.OptionValue.builder().setLabel("OR").setValue("OR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("PW").setValue("PW").build(),
        SelectWithLabel.OptionValue.builder().setLabel("PA").setValue("PA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("PR").setValue("PR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("RI").setValue("RI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("SC").setValue("SC").build(),
        SelectWithLabel.OptionValue.builder().setLabel("SD").setValue("SD").build(),
        SelectWithLabel.OptionValue.builder().setLabel("TN").setValue("TN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("TX").setValue("TX").build(),
        SelectWithLabel.OptionValue.builder().setLabel("UT").setValue("UT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("VT").setValue("VT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("VI").setValue("VI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("VA").setValue("VA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("WA").setValue("WA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("WV").setValue("WV").build(),
        SelectWithLabel.OptionValue.builder().setLabel("WI").setValue("WI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("WY").setValue("WY").build());
  }
}
