package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;
import views.components.TextFormatter;

/**
 * Represents a static content question in the context of a specific applicant.
 *
 * <p>Static content question doesn't have an answer. This class is the boilerplate needed to fit
 * static content in the question framework.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class StaticContentQuestion extends AbstractQuestion {

  StaticContentQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of();
  }

  @Override
  public boolean isAnswered() {
    return true;
  }

  @Override
  public String getAnswerString() {
    return "";
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of();
  }

  public String getFormattedTextForRendering(String ariaLabel) {
    return TextFormatter.formatTextToSanitizedHTMLWithAriaLabel(
        applicantQuestion.getQuestionText(),
        /* preserveEmptyLines= */ true,
        /* addRequiredIndicator= */ false,
        ariaLabel);
  }
}
