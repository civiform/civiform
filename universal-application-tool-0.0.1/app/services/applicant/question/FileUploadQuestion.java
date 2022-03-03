package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a file upload question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class FileUploadQuestion implements Question {

  private final ApplicantQuestion applicantQuestion;
  private Optional<String> fileKeyValue;
  private Optional<String> originalFileNameValue;

  public FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public boolean hasConditionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // We can't predict ahead of time what the path will be.
    return ImmutableList.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a file upload question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getFileKeyPath());
  }

  public ValidationErrorMessage fileRequiredMessage() {
    return ValidationErrorMessage.create(MessageKey.FILEUPLOAD_VALIDATION_FILE_REQUIRED);
  }

  public Optional<String> getFileKeyValue() {
    if (fileKeyValue != null) {
      return fileKeyValue;
    }

    fileKeyValue = applicantQuestion.getApplicantData().readString(getFileKeyPath());

    return fileKeyValue;
  }

  public Optional<String> getOriginalFileName() {
    if (originalFileNameValue != null) {
      return originalFileNameValue;
    }
    originalFileNameValue =
        applicantQuestion.getApplicantData().readString(getOriginalFileNamePath());
    return originalFileNameValue;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.FILEUPLOAD)) {
      throw new RuntimeException(
          String.format(
              "Question is not a FILEUPLOAD question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public FileUploadQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (FileUploadQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getFileKeyPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.FILE_KEY);
  }

  public Path getOriginalFileNamePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ORIGINAL_FILE_NAME);
  }

  public Optional<String> getFilename() {
    if (!isAnswered() || getFileKeyValue().isEmpty()) {
      return Optional.empty();
    }
    return getFileKeyValue().map(key -> key.split("/", 4)).map(arr -> arr[arr.length - 1]);
  }

  @Override
  public String getAnswerString() {
    if (getFilename().isEmpty()) {
      return "-- NO FILE SELECTED --";
    }
    return String.format(
        "-- %s UPLOADED (click to download) --",
        getOriginalFileName().isPresent() ? getOriginalFileName().get() : getFilename().get());
  }
}
