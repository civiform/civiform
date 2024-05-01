package views.questiontypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.EmailQuestion;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;

/** Renders an email question. */
public class EmailQuestionRenderer extends ApplicantSingleQuestionRenderer {

  public EmailQuestionRenderer(ApplicantQuestion question) {
    super(question);
  }

  @Override
  public String getReferenceClass() {
    return ReferenceClasses.EMAIL_QUESTION;
  }

  @Override
  protected DivTag renderInputTag(
      ApplicantQuestionRendererParams params,
      ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> validationErrors,
      ImmutableList<String> ariaDescribedByIds,
      boolean isOptional) {
    EmailQuestion emailQuestion = applicantQuestion.createEmailQuestion();

    FieldWithLabel emailField =
        FieldWithLabel.email()
            .setFieldName(emailQuestion.getEmailPath().toString())
            .setAutocomplete(Optional.of("email"))
            .setValue(emailQuestion.getEmailValue().orElse(""))
            .setAttribute("inputmode", "email")
            .setAriaRequired(!isOptional)
            .setFieldErrors(
                params.messages(),
                validationErrors.getOrDefault(emailQuestion.getEmailPath(), ImmutableSet.of()))
            .setAriaDescribedByIds(ariaDescribedByIds)
            .setScreenReaderText(applicantQuestion.getQuestionTextForScreenReader());

    if (params.autofocusSingleField()) {
      emailField.focusOnInput();
    }

    if (!validationErrors.isEmpty()) {
      emailField.forceAriaInvalid();
    }

    return emailField.getEmailTag();
  }
}
