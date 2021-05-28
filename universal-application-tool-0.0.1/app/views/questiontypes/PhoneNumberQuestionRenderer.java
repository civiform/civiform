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

    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(phoneNumberQuestion.getPhoneNumberPath().toString())
            .setValue(phoneNumberQuestion.getPhoneNumberValue().orElse(""))
            .setFieldErrors(params.messages(), phoneNumberQuestion.getQuestionErrors())
            .getContainer();

    return renderInternal(params.messages(), questionFormContent, false);
  }
}
