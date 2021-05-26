package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EmailQuestion;
import views.components.FieldWithLabel;

public class EmailQuestionRenderer extends ApplicantQuestionRenderer {

  public EmailQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public Tag render(ApplicantQuestionRendererParams params) {
    EmailQuestion emailQuestion = question.createEmailQuestion();

    Tag questionFormContent =
        FieldWithLabel.input()
            .setFieldName(emailQuestion.getEmailPath().toString())
            .setValue(emailQuestion.getEmailValue().orElse(""))
            .setFieldErrors(params.messages(), emailQuestion.getQuestionErrors())
            .getContainer();

    return renderInternal(params.messages(), questionFormContent, false);
  }
}
