package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.Path;

public class FileUploadQuestionDefinition extends QuestionDefinition {

  public FileUploadQuestionDefinition(
      OptionalLong id,
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        id,
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        FileUploadValidationPredicates.create());
  }

  public FileUploadQuestionDefinition(
      String name,
      Path path,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        name,
        path,
        enumeratorId,
        description,
        questionText,
        questionHelpText,
        FileUploadValidationPredicates.create());
  }

  @AutoValue
  public abstract static class FileUploadValidationPredicates extends ValidationPredicates {

    public static FileUploadValidationPredicates parse(String jsonString) {
      try {
        return mapper.readValue(
            jsonString,
            AutoValue_FileUploadQuestionDefinition_FileUploadValidationPredicates.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    public static FileUploadValidationPredicates create() {
      return new AutoValue_FileUploadQuestionDefinition_FileUploadValidationPredicates();
    }
  }

  public FileUploadValidationPredicates getFileUploadValidationPredicates() {
    return (FileUploadValidationPredicates) getValidationPredicates();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.FILEUPLOAD;
  }

  @Override
  ImmutableMap<Path, ScalarType> getScalarMap() {
    return ImmutableMap.of(getFileKeyPath(), getFileKeyType());
  }

  public Path getFileKeyPath() {
    return getPath().join("file_key");
  }

  public ScalarType getFileKeyType() {
    return ScalarType.STRING;
  }
}
