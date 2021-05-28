package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.PhoneNumberQuestion;
import views.components.FieldWithLabel;

public class PhoneNumberQuestionRenderer extends ApplicantQuestionRenderer {

  public PhoneNumberQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    PhoneNumberQuestion phoneNumberQuestion = question.createPhoneNumberQuestion();

    // TODO: Add <input type="tel"> using J2Html
    // with validation pattern="[0-9]{3}-[0-9]{3}-[0-9]{4}"
    // https://github.com/seattle-uat/civiform/issues/452
    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(phoneNumberQuestion.getPhoneNumberPath().toString())
            .setFieldType("tel")
            .setValue(phoneNumberQuestion.getPhoneNumberValue().orElse(""))
            .setFieldErrors(params.messages(), phoneNumberQuestion.getQuestionErrors())
            .getContainer();

    return renderInternal(params.messages(), questionFormContent, false);
  }
}
