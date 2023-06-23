package services.question.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines a file upload question. */
public final class FileUploadQuestionDefinition extends QuestionDefinition {

  public FileUploadQuestionDefinition(
      OptionalLong id,
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText,
      Optional<Instant> lastModifiedTime) {
    super(
        QuestionDefinitionConfig.builder()
            .setId(id)
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(FileUploadValidationPredicates.create())
            .setLastModifiedTime(lastModifiedTime)
            .build());
  }

  public FileUploadQuestionDefinition(
      String name,
      Optional<Long> enumeratorId,
      String description,
      LocalizedStrings questionText,
      LocalizedStrings questionHelpText) {
    super(
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setEnumeratorId(enumeratorId)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setValidationPredicates(FileUploadValidationPredicates.create())
            .build());
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
}
