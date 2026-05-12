package services.applicant.question;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import models.StoredFileModel;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsString;
import play.api.libs.json.JsValue;
import play.libs.Scala;
import repository.StoredFileRepository;
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
   * Returns the stored original filenames, if they exist in storage.
   */
  public Optional<ImmutableList<String>> getOriginalFileNameListValue() {
    return applicantQuestion.getApplicantData().readStringList(getOriginalFileNameListPath());
  }

  /*
   * Returns the stored original filename for the given index, if the data exists in storage.
   */
  public Optional<String> getOriginalFileNameValueForIndex(int index) {
    Optional<String> originalFileName =
        applicantQuestion.getApplicantData().readString(getOriginalFileNameListPathForIndex(index));
    return originalFileName;
  }

  /*
   * Returns the filename stored at the given index, will search for a value to return from
   * the original filename column first, and if none is found, then it will return the filekey.
   */
  public Optional<String> getFileNameForIndex(int index) {
    Optional<String> fileNameOptional = getOriginalFileNameValueForIndex(index);
    return fileNameOptional.isPresent()
        ? fileNameOptional
        : getFileKeyValueForIndex(index).map(FileUploadQuestion::getFileName);
  }

  /**
   * Returns a JSON array of display names for each non-empty file key, in list order. Matches the
   * file rows rendered in {@code FileUploadQuestionFragment} (empty keys are omitted). Used by
   * {@code file_upload.ts} for client-side file name de-duplication.
   */
  public JsArray getUploadedFileData() {
    ImmutableList<String> keys = getFileKeyListValue().orElse(ImmutableList.of());

    ImmutableList<JsValue> fileNames =
        IntStream.range(0, keys.size())
            .filter(
                i -> {
                  String key = keys.get(i);
                  return key != null && !key.isEmpty();
                })
            .mapToObj(i -> new JsString(getFileNameForIndex(i).orElse(keys.get(i))))
            .collect(toImmutableList());

    return new JsArray(Scala.toSeq(fileNames));
  }

  /**
   * Looks up the original file name for a given file key by querying the database for the
   * corresponding {@link StoredFileModel}.
   */
  public static CompletionStage<Optional<String>> getOriginalFileNameForFileKey(
      StoredFileRepository storedFileRepository, String fileKey) {
    return storedFileRepository
        .lookupFile(fileKey)
        .thenApply(maybeFile -> maybeFile.flatMap(StoredFileModel::getOriginalFileName));
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

  /**
   * Builds form data that preserves all existing file keys and original file names, then appends a
   * new entry at the end.
   */
  public ImmutableMap<String, String> buildFormDataForAdd(String newFileKey, String newFileName) {
    ImmutableMap.Builder<String, String> formData = new ImmutableMap.Builder<>();
    Optional<ImmutableList<String>> keysOptional = getFileKeyListValue();
    Optional<ImmutableList<String>> originalFileNamesOptional = getOriginalFileNameListValue();
    int newIndex = keysOptional.map(ImmutableList::size).orElse(0);

    // Preserve existing file keys.
    if (keysOptional.isPresent()) {
      for (int i = 0; i < keysOptional.get().size(); i++) {
        formData.put(getFileKeyListPathForIndex(i).toString(), keysOptional.get().get(i));
      }
    }

    // Preserve existing original file names.
    if (originalFileNamesOptional.isPresent()) {
      for (int i = 0; i < originalFileNamesOptional.get().size(); i++) {
        formData.put(
            getOriginalFileNameListPathForIndex(i).toString(),
            originalFileNamesOptional.get().get(i));
      }
    }

    // Append new file key and original file name.
    formData.put(getFileKeyListPathForIndex(newIndex).toString(), newFileKey);
    formData.put(getOriginalFileNameListPathForIndex(newIndex).toString(), newFileName);

    return formData.build();
  }

  /**
   * Builds form data that preserves all existing file keys and original file names, but blanks out
   * the entry matching {@code fileKeyToRemove}.
   */
  public ImmutableMap<String, String> buildFormDataForRemove(String fileKeyToRemove) {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    Optional<ImmutableList<String>> keys = getFileKeyListValue();
    Optional<ImmutableList<String>> names = getOriginalFileNameListValue();
    int removedIndex = -1;

    if (keys.isPresent()) {
      for (int i = 0; i < keys.get().size(); i++) {
        String keyValue = keys.get().get(i);
        boolean remove = keyValue.equals(fileKeyToRemove);
        if (remove) {
          removedIndex = i;
        }
        formData.put(getFileKeyListPathForIndex(i).toString(), remove ? "" : keyValue);
      }
    }

    if (names.isPresent() && removedIndex >= 0) {
      for (int i = 0; i < names.get().size(); i++) {
        formData.put(
            getOriginalFileNameListPathForIndex(i).toString(),
            i == removedIndex ? "" : names.get().get(i));
      }
    }

    return formData.build();
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

  /**
   * Suggested download file name for presigned GETs: non-blank original from {@link
   * StoredFileModel} when available, otherwise {@link #getFileName(String)}.
   *
   * <p>The result is always non-empty so storage clients can set {@code Content-Disposition}.
   */
  public static Optional<String> getUploadedFileName(
      Optional<StoredFileModel> storedFile, String fileKey) {
    return storedFile
        .flatMap(StoredFileModel::getOriginalFileName)
        .filter(s -> !s.isBlank())
        .or(() -> Optional.of(getFileName(fileKey)));
  }

  // Matches a filename with an extension.
  private static final Pattern FILE_NAME_REGEX = Pattern.compile("(.*)(\\.[^.]+)$");

  /**
   * Returns a name derived from {@code name} that does not appear in {@code existingNames},
   * appending a "-N" numeric suffix as needed.
   *
   * <p>This method runs after the file has already been uploaded to cloud storage under a
   * UUID-based key. The returned name is for display only and is stored in {@link
   * services.applicant.ApplicantData} alongside the file key, and is what the applicant sees in the
   * UI. The file key is the source of truth for retrieval and ACL lookups (via {@link
   * models.StoredFileModel}).
   */
  public static String getUniqueName(String name, ImmutableList<String> existingNames) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("name is null or empty");
    }

    if (existingNames == null) {
      throw new IllegalArgumentException("existingNames is null");
    }

    if (!existingNames.contains(name)) {
      return name;
    }

    Matcher extMatcher = FILE_NAME_REGEX.matcher(name);
    boolean hasRealExtension = extMatcher.matches() && !extMatcher.group(1).isEmpty();
    String base = hasRealExtension ? extMatcher.group(1) : name;
    String ext = hasRealExtension ? extMatcher.group(2) : "";

    long counter = 2;
    while (existingNames.contains(name)) {
      name = base + "-" + counter + ext;
      counter++;
    }
    return name;
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
