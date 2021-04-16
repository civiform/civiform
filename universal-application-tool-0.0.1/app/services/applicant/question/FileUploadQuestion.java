package services.applicant.question;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionType;

public class FileUploadQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> fileKeyValue;

  public FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    // There are no inherent requirements in a file upload question.
    return false;
  }

  @Override
  public boolean isAnswered() {
    // TODO(https://github.com/seattle-uat/civiform/issues/783): Use hydrated path.
    return applicantQuestion.getApplicantData().hasPath(getFileKeyPath());
  }

  public Optional<String> getFileKeyValue() {
    if (fileKeyValue != null) {
      return fileKeyValue;
    }

    fileKeyValue = applicantQuestion.getApplicantData().readString(getFileKeyPath());

    return fileKeyValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.FILE_UPLOAD)) {
      throw new RuntimeException(
          String.format(
              "Question is not a FILE_UPLOAD question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getPath(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public FileUploadQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (FileUploadQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getFileKeyPath() {
    return getQuestionDefinition().getFileKeyPath();
  }
}
