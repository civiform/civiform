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
public class FileUploadQuestion extends QuestionImpl {

  // This value is serving double duty as a singleton load of the value.
  // This value is an optional of an optional because not all questions are file upload questions,
  // and if they are this value could still not be set.
  private Optional<Optional<String>> fileKeyValueCache;
  private Optional<Optional<String>> originalFileNameValueCache;

  public FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    this.fileKeyValueCache = Optional.empty();
    this.originalFileNameValueCache = Optional.empty();
  }

  @Override
  protected ImmutableSet<QuestionType> validQuestionTypes() {
    return ImmutableSet.of(QuestionType.FILEUPLOAD);
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    // TODO: Implement admin-defined validation.
    return ImmutableSet.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a file upload question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    // TODO(#1944): Consider adding getFileKeyPath to getAllPaths.
    // Adding it currently would cause the value to start being exported
    // by the demographics exporter.
    return applicantQuestion.getApplicantData().hasPath(getFileKeyPath());
  }

  public ValidationErrorMessage fileRequiredMessage() {
    return ValidationErrorMessage.create(MessageKey.FILEUPLOAD_VALIDATION_FILE_REQUIRED);
  }

  public Optional<String> getFileKeyValue() {
    if (fileKeyValueCache.isPresent()) {
      return fileKeyValueCache.get();
    }

    fileKeyValueCache =
        Optional.of(applicantQuestion.getApplicantData().readString(getFileKeyPath()));

    return fileKeyValueCache.get();
  }

  public Optional<String> getOriginalFileName() {
    if (originalFileNameValueCache.isPresent()) {
      return originalFileNameValueCache.get();
    }

    originalFileNameValueCache =
        Optional.of(applicantQuestion.getApplicantData().readString(getOriginalFileNamePath()));

    return originalFileNameValueCache.get();
  }

  public FileUploadQuestionDefinition getQuestionDefinition() {
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

    String displayFileName =
        getOriginalFileName().isPresent() ? getOriginalFileName().get() : getFilename().get();

    return String.format("-- %s UPLOADED (click to download) --", displayFileName);
  }
}
