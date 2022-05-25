package views.questiontypes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import services.Path;
import services.applicant.ValidationErrorMessage;
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
  protected Tag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors) {
    EmailQuestion emailQuestion = question.createEmailQuestion();

    Tag questionFormContent =
        FieldWithLabel.email()
            .setFieldName(emailQuestion.getEmailPath().toString())
            .setValue(emailQuestion.getEmailValue().orElse(""))
            .setDescriptionId(getDescriptionId())
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(emailQuestion.getEmailPath(), ImmutableSet.of()))
            .setScreenReaderText(question.getQuestionText())
            .getContainer();

    return questionFormContent;
  }
}
