package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EmailQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders an email question. */
public class EmailQuestionRenderer extends ApplicantQuestionRendererImpl {

  public EmailQuestionRenderer(ApplicantQuestion question) {
    super(question, InputFieldType.SINGLE);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.EMAIL_QUESTION;
  }

  @Override
  protected DivTag renderTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    EmailQuestion emailQuestion = question.createEmailQuestion();

    DivTag questionFormContent =
        FieldWithLabel.email()
            .setFieldName(emailQuestion.getEmailPath().toString())
            .setValue(emailQuestion.getEmailValue().orElse(""))
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(emailQuestion.getEmailPath(), ImmutableSet.of()))
            .setAriaInvalid(hasErrors)
            .setAriaDescribedByIds(ariaDescribedByIds)
            .setScreenReaderText(question.getQuestionText())
            .getEmailTag();

    return questionFormContent;
  }
}
