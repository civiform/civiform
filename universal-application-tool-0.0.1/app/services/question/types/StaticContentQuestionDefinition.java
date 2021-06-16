package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.LocalizedStrings;

public class StaticContentQuestionDefinition extends QuestionDefinition {

    public StaticContentQuestionDefinition(
        OptionalLong id,
        String name,
        Optional<Long> enumeratorId,
        String description,
        LocalizedStrings questionText,
        LocalizedStrings questionHelpText,
        TextValidationPredicates validationPredicates) {
      super(
          id, name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    }
    
    public StaticContentQuestionDefinition(
        String name,
        Optional<Long> enumeratorId,
        String description,
        LocalizedStrings questionText,
        LocalizedStrings questionHelpText,
        TextValidationPredicates validationPredicates) {
      super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    }
  
    public StaticContentQuestionDefinition(
        String name,
        Optional<Long> enumeratorId,
        String description,
        LocalizedStrings questionText,
        LocalizedStrings questionHelpText) {
      super(
          name,
          enumeratorId,
          description,
          questionText,
          questionHelpText,
          TextValidationPredicates.create());
    }
  
    @Override
    public QuestionType getQuestionType() {
      return QuestionType.STATIC;
    }
  }
  