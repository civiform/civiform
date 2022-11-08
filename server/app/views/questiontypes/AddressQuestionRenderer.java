package views.questiontypes;

import static j2html.TagCreator.div;

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
                            .setLabel("Pick your State")
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
                        stateField.getSelectTag(),
                        zipField.getInputTag()));

    return addressQuestionFormContent;
  }

  private static ImmutableList<SelectWithLabel.OptionValue> stateOptions() {
    return ImmutableList.of(
        SelectWithLabel.OptionValue.builder().setLabel("Alabama  - AL").setValue("AL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Alaska - AK").setValue("AK").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("American Samoa - AS")
            .setValue("AS")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("Arizona - AZ").setValue("AZ").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Arkansas - AR").setValue("AR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("California - CA").setValue("CA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Colorado - CO").setValue("CO").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Connecticut - CT").setValue("CT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Delaware - DE").setValue("DE").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("District of Columbia - DC")
            .setValue("DC")
            .build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("Federated States of Micronesia - FM")
            .setValue("FM")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("Florida - FL").setValue("FL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Georgia - GA").setValue("GA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Guam - GU").setValue("GU").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Hawaii - HI").setValue("HI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Idaho - ID").setValue("ID").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Illinois - IL").setValue("IL").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Indiana - IN").setValue("IN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Iowa - IA").setValue("IA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Kansas - KS").setValue("KS").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Kentucky - KY").setValue("KY").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Louisiana - LA").setValue("LA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Maine - ME").setValue("ME").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("Marshall Islands - MH")
            .setValue("MH")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("Maryland - MD").setValue("MD").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Massachusetts - MA").setValue("MA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Michigan - MI").setValue("MI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Minnesota - MN").setValue("MN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Mississippi - MS").setValue("MS").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Missouri - MO").setValue("MO").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Montana - MT").setValue("MT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Nebraska - NE").setValue("NE").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Nevada - NV").setValue("NV").build(),
        SelectWithLabel.OptionValue.builder().setLabel("New Hampshire - NH").setValue("NH").build(),
        SelectWithLabel.OptionValue.builder().setLabel("New Jersey - NJ").setValue("NJ").build(),
        SelectWithLabel.OptionValue.builder().setLabel("New Mexico - NM").setValue("NM").build(),
        SelectWithLabel.OptionValue.builder().setLabel("New York - NY").setValue("NY").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("North Carolina - NC")
            .setValue("NC")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("North Dakota - ND").setValue("ND").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("Northern Mariana Islands - MP")
            .setValue("MP")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("Ohio - OH").setValue("OH").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Oklahoma - OK").setValue("OK").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Oregon - OR").setValue("OR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Palau - PW").setValue("PW").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Pennsylvania - PA").setValue("PA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Puerto Rico - PR").setValue("PR").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Rhode Island - RI").setValue("RI").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("South Carolina - SC")
            .setValue("SC")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("South Dakota - SD").setValue("SD").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Tennessee - TN").setValue("TN").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Texas - TX").setValue("TX").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Utah - UT").setValue("UT").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Vermont - VT").setValue("VT").build(),
        SelectWithLabel.OptionValue.builder()
            .setLabel("Virgin Islands - VI")
            .setValue("VI")
            .build(),
        SelectWithLabel.OptionValue.builder().setLabel("Virginia - VA").setValue("VA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Washington - WA").setValue("WA").build(),
        SelectWithLabel.OptionValue.builder().setLabel("West Virginia - WV").setValue("WV").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Wisconsin - WI").setValue("WI").build(),
        SelectWithLabel.OptionValue.builder().setLabel("Wyoming - WY").setValue("WY").build());
  }
}
