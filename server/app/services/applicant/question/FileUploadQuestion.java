package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.FileUploadQuestionDefinition;

/**
 * Represents a file upload question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public final class FileUploadQuestion extends AbstractQuestion {

  // This value is serving double duty as a singleton load of the value.
  // This value is an optional of an optional because not all questions are file upload questions,
  // and if they are this value could still not be set.
  private Optional<Optional<String>> fileKeyValueCache;
  private Optional<Optional<String>> originalFileNameValueCache;

  FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    this.fileKeyValueCache = Optional.empty();
    this.originalFileNameValueCache = Optional.empty();
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    // TODO: Implement admin-defined validation.
    // TODO(#1944): Validate that the file has been uploaded.
    return ImmutableMap.of();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getFileKeyPath(), getFileKeyListPath());
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

  public Optional<ImmutableList<String>> getFileKeyListValue() {
    return applicantQuestion.getApplicantData().readStringList(getFileKeyListPath());
  }

  /**
   * Returns {@code true} if an additional file can be added according to the maximum files set on
   * the current question definition.
   */
  public boolean canUploadFile() {
    // Max can't be zero, so if there are no values, we can always upload a new one.
    if (!getFileKeyListValue().isPresent()) {
      return true;
    }

    // No max, so we can always upload.
    if (!getQuestionDefinition().getMaxFiles().isPresent()) {
      return true;
    }

    return getFileKeyListValue().get().size() < getQuestionDefinition().getMaxFiles().getAsInt();
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

  public Path getFileKeyListPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.FILE_KEY_LIST);
  }

  public Path getFileKeyListPathForIndex(int index) {
    return applicantQuestion
        .getContextualizedPath()
        .join(Scalar.FILE_KEY_LIST)
        .asArrayElement()
        .atIndex(index);
  }

  public Path getOriginalFileNamePath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ORIGINAL_FILE_NAME);
  }

  public Optional<String> getFilename() {
    if (!isAnswered() || getFileKeyValue().isEmpty()) {
      return Optional.empty();
    }
    return getFileKeyValue().map(FileUploadQuestion::getFileName);
  }

  public static String getFileName(String fileKey) {
    String[] parts = fileKey.split("/", 4);
    return parts[parts.length - 1];
  }

  @Override
  public String getAnswerString() {
    if (getFilename().isEmpty()) {
      return getDefaultAnswerString();
    }

    String displayFileName =
        getOriginalFileName().isPresent() ? getOriginalFileName().get() : getFilename().get();

    return String.format("-- %s UPLOADED (click to download) --", displayFileName);
  }

  @Override
  public String getDefaultAnswerString() {
    return "-- NO FILE SELECTED --";
  }
}
