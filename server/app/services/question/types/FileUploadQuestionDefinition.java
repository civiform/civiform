package services.question.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.OptionalInt;

/** Defines a file upload question. */
public final class FileUploadQuestionDefinition extends QuestionDefinition {

  public FileUploadQuestionDefinition(@JsonProperty("config") QuestionDefinitionConfig config) {
    super(config);
  }

  @JsonDeserialize(
      builder = AutoValue_FileUploadQuestionDefinition_FileUploadValidationPredicates.Builder.class)
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

    @JsonProperty("maxFiles")
    public abstract OptionalInt maxFiles();

    public static FileUploadValidationPredicates create() {
      return builder().setMaxFiles(OptionalInt.empty()).build();
    }

    public static Builder builder() {
      return new AutoValue_FileUploadQuestionDefinition_FileUploadValidationPredicates.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      @JsonProperty("maxFiles")
      public abstract Builder setMaxFiles(OptionalInt allowMultipleUpload);

      public abstract FileUploadValidationPredicates build();
    }
  }

  public OptionalInt getMaxFiles() {
    return getFileUploadValidationPredicates().maxFiles();
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.FILEUPLOAD;
  }

  @Override
  ValidationPredicates getDefaultValidationPredicates() {
    return FileUploadValidationPredicates.create();
  }

  @JsonIgnore
  private FileUploadValidationPredicates getFileUploadValidationPredicates() {
    return (FileUploadValidationPredicates) getValidationPredicates();
  }
}
