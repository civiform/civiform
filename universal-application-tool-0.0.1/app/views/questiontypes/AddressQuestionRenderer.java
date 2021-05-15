package views.questiontypes;

import static j2html.TagCreator.div;

import j2html.tags.Tag;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.question.AddressQuestion;
import services.applicant.question.ApplicantQuestion;
import views.components.FieldWithLabel;
import views.style.Styles;

public class AddressQuestionRenderer extends ApplicantQuestionRenderer {

  public AddressQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
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
                    .setFieldErrors(messages, addressQuestion.getStreetErrors())
                    .getContainer(),
                /** Second line of address entry: Address line 2 AKA apartment, unit, etc. */
                FieldWithLabel.input()
                    .setFieldName(addressQuestion.getLine2Path().toString())
                    .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_LINE_2.getKeyName()))
                    .setPlaceholderText(
                        messages.at(MessageKey.ADDRESS_PLACEHOLDER_LINE_2.getKeyName()))
                    .setValue(addressQuestion.getLine2Value().orElse(""))
                    .getContainer(),
                /** Third line of address entry: City, State, Zip */
                div()
                    .withClasses(Styles.GRID, Styles.GRID_COLS_3, Styles.GAP_2)
                    .with(
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getCityPath().toString())
                            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_CITY.getKeyName()))
                            .setValue(addressQuestion.getCityValue().orElse(""))
                            .setFieldErrors(messages, addressQuestion.getCityErrors())
                            .getContainer(),
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getStatePath().toString())
                            .setLabelText(messages.at(MessageKey.ADDRESS_LABEL_STATE.getKeyName()))
                            .setValue(addressQuestion.getStateValue().orElse(""))
                            .setFieldErrors(messages, addressQuestion.getStateErrors())
                            .getContainer(),
                        FieldWithLabel.input()
                            .setFieldName(addressQuestion.getZipPath().toString())
                            .setLabelText(
                                messages.at(MessageKey.ADDRESS_LABEL_ZIPCODE.getKeyName()))
                            .setValue(addressQuestion.getZipValue().orElse(""))
                            .setFieldErrors(messages, addressQuestion.getZipErrors())
                            .getContainer()));

    return renderInternal(messages, addressQuestionFormContent);
  }
}
