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
public final class FileUploadQuestion extends Question {

  // There is an unfortunate dual logic in the how filenames are stored for question answers. AWS
  // deployments store the filename in the key, Azure stores them in a separate column.
  //
  // In order for the application to work properly we must look for data in the original file names
  // Scalar, and absent that data, return the file keys as the filename.
  //
  // Remember whether to use original filenames, to avoid multiple reads for AWS deployments.
  private boolean useOriginalFileNames;

  // This value is serving double duty as a singleton load of the value.
  // This value is an optional of an optional because not all questions are file upload questions,
  // and if they are this value could still not be set.
  private Optional<Optional<String>> fileKeyValueCache;
  private Optional<Optional<String>> originalFileNameValueCache;

  FileUploadQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    this.useOriginalFileNames = true;
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
    return ImmutableList.of(
        getFileKeyPath(),
        getFileKeyListPath(),
        getOriginalFileNamePath(),
        getOriginalFileNameListPath());
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

  public Optional<String> getFileKeyValueForIndex(int index) {
    return applicantQuestion.getApplicantData().readString(getFileKeyListPathForIndex(index));
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

  /*
   * Returns the stored original filenames. If this data does not exist, return the file key values.
   */
  public Optional<ImmutableList<String>> getOriginalFileNameListValue() {
    if (useOriginalFileNames) {
      Optional<ImmutableList<String>> originalFileNames =
          applicantQuestion.getApplicantData().readStringList(getOriginalFileNameListPath());
      if (originalFileNames.isPresent()) {
        return originalFileNames;
      }
    }

    useOriginalFileNames = false;
    return getFileKeyListValue();
  }

  /*
   * Returns the stored original filename for the given index. If this data does not exist, return the
   * file key value for the same index.
   */
  public Optional<String> getOriginalFileNameValueForIndex(int index) {
    if (useOriginalFileNames) {
      Optional<String> originalFileName =
          applicantQuestion
              .getApplicantData()
              .readString(getOriginalFileNameListPathForIndex(index));
      if (originalFileName.isPresent()) {
        return originalFileName;
      }
    }

    // Execution will reach this point, if we have no data for original file names.
    useOriginalFileNames = false;
    return getFileKeyValueForIndex(index);
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

  public Path getOriginalFileNameListPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.ORIGINAL_FILE_NAME_LIST);
  }

  public Path getOriginalFileNameListPathForIndex(int index) {
    return applicantQuestion
        .getContextualizedPath()
        .join(Scalar.ORIGINAL_FILE_NAME_LIST)
        .asArrayElement()
        .atIndex(index);
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
