package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.LocalizedStrings;

/** Defines an id question. */
public class IDQuestionDefinition extends QuestionDefinition {

    public IDQuestionDefinition(
            OptionalLong id,
            String name,
            Optional<Long> enumeratorId,
            String description,
            LocalizedStrings questionText,
            LocalizedStrings questionHelpText,
            IDValidationPredicates validationPredicates) {
        super(
                id, name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    }

    public IDQuestionDefinition(
            String name,
            Optional<Long> enumeratorId,
            String description,
            LocalizedStrings questionText,
            LocalizedStrings questionHelpText,
            IDValidationPredicates validationPredicates) {
        super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    }

    public IDQuestionDefinition(
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
                IDValidationPredicates.create());
    }

    @JsonDeserialize(
            builder = AutoValue_IDQuestionDefinition_IDValidationPredicates.Builder.class)
    @AutoValue
    public abstract static class IDValidationPredicates extends ValidationPredicates {

        public static IDValidationPredicates parse(String jsonString) {
            try {
                return mapper.readValue(
                        jsonString, AutoValue_IDQuestionDefinition_IDValidationPredicates.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public static IDValidationPredicates create() {
            return builder().build();
        }

        public static IDValidationPredicates create(int minLength, int maxLength) {
            return builder().setMinLength(minLength).setMaxLength(maxLength).build();
        }

        @JsonProperty("minLength")
        public abstract OptionalInt minLength();

        @JsonProperty("maxLength")
        public abstract OptionalInt maxLength();

        public static Builder builder() {
            return new AutoValue_IDQuestionDefinition_IDValidationPredicates.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {

            @JsonProperty("minLength")
            public abstract Builder setMinLength(OptionalInt minLength);

            public abstract Builder setMinLength(int minLength);

            @JsonProperty("maxLength")
            public abstract Builder setMaxLength(OptionalInt maxLength);

            public abstract Builder setMaxLength(int maxLength);

            public abstract IDValidationPredicates build();
        }
    }

    public IDValidationPredicates getIDValidationPredicates() {
        return (IDValidationPredicates) getValidationPredicates();
    }

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.ID;
    }

    public OptionalInt getMinLength() {
        return getIDValidationPredicates().minLength();
    }

    public OptionalInt getMaxLength() {
        return getIDValidationPredicates().maxLength();
    }
}
