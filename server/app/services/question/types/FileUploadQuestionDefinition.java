package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;

/** Defines a file upload question. */
public final class FileUploadQuestionDefinition extends QuestionDefinition {

  public FileUploadQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
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
  ValidationPredicates getDefaultValidationPredicates() {
    return FileUploadValidationPredicates.create();
  }
}
