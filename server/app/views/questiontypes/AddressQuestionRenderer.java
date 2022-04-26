package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

/** Renders an address question. */
public class AddressQuestionRenderer extends ApplicantQuestionRendererImpl {

  public AddressQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.ADDRESS_QUESTION;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    Messages messages = params.messages();
    AddressQuestion addressQuestion = question.createAddressQuestion();

    Tag addressQuestionFormContent =
        div()
            .with(
                /** First line of address entry: Address line 1 AKA street address */
                FieldWithLabel.input()
                    .setFieldName(addressQuestion.getStreetPath().toString())
                    .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STREET.getKeyName()))
                    .setPlaceholderText(
                        messages.at(MessageKey.ADDRESS_PLACEHOLDER_STREET.getKeyName()))
                    .setValue(addressQuestion.getStreetValue().orElse(""))
                    .setFieldErrors(messages, addressQuestion.getStreetErrorMessage())
                    .showFieldErrors(!addressQuestion.getStreetErrors().isEmpty())
                    .addReferenceClass(ReferenceClasses.ADDRESS_STREET_1)
                    .getContainer(),
                /** Second line of address entry: Address line 2 AKA apartment, unit, etc. */
                FieldWithLabel.input()
                    .setFieldName(addressQuestion.getLine2Path().toString())
                    .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_LINE_2.getKeyName()))
                    .setPlaceholderText(
                        messages.at(MessageKey.ADDRESS_PLACEHOLDER_LINE_2.getKeyName()))
                    .setValue(addressQuestion.getLine2Value().orElse(""))
                    .addReferenceClass(ReferenceClasses.ADDRESS_STREET_2)
                    .getContainer(),
                /** Third line of address entry: City, State, Zip */
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_3, Styles.GAP_2)
                    .with(
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getCityPath().toString())
                            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_CITY.getKeyName()))
                            .setValue(addressQuestion.getCityValue().orElse(""))
                            .addReferenceClass(ReferenceClasses.ADDRESS_CITY)
                            .setFieldErrors(messages, addressQuestion.getCityErrorMessage())
                            .showFieldErrors(!addressQuestion.getCityErrors().isEmpty())
                            .getContainer(),
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getStatePath().toString())
                            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STATE.getKeyName()))
                            .setValue(addressQuestion.getStateValue().orElse(""))
                            .setFieldErrors(messages, addressQuestion.getStateErrorMessage())
                            .showFieldErrors(!addressQuestion.getStateErrors().isEmpty())
                            .addReferenceClass(ReferenceClasses.ADDRESS_STATE)
                            .getContainer(),
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getZipPath().toString())
                            .setLabelText(
                                messages.at(MessageKey.ADDRESS_LABEL_ZIPCODE.getKeyName()))
                            .setValue(addressQuestion.getZipValue().orElse(""))
                            .setFieldErrors(messages, addressQuestion.getZipErrorMessage())
                            .showFieldErrors(!addressQuestion.getZipErrors().isEmpty())
                            .addReferenceClass(ReferenceClasses.ADDRESS_ZIP)
                            .getContainer()));

    return addressQuestionFormContent;
  }
}
