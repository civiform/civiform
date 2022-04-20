package views.questiontypes;

import j2html.tags.Tag;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EmailQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders an email question. */
public class EmailQuestionRenderer extends ApplicantQuestionRendererImpl {

  public EmailQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.EMAIL_QUESTION;
  }

  @Override
  protected boolean shouldDisplayQuestionErrors() {
      return false;
  }

  @Override
  protected Tag renderTag(ApplicantQuestionRendererParams params) {
    EmailQuestion emailQuestion = question.createEmailQuestion();

    Tag questionFormContent =
        FieldWithLabel.email()
            .setFieldName(emailQuestion.getEmailPath().toString())
            .setValue(emailQuestion.getEmailValue().orElse(""))
            .setFieldErrors(params.messages(), emailQuestion.getQuestionErrors())
            .setScreenReaderText(question.getQuestionText())
            .getContainer();

    return questionFormContent;
  }
}
