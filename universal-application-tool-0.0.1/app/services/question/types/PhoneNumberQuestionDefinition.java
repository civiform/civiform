package services.question.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.LocalizedStrings;

public class PhoneNumberQuestionDefinition extends QuestionDefinition {

    public PhoneNumberQuestionDefinition(
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

    public PhoneNumberQuestionDefinition(
            String name,
            Optional<Long> enumeratorId,
            String description,
            LocalizedStrings questionText,
            LocalizedStrings questionHelpText,
            TextValidationPredicates validationPredicates) {
        super(name, enumeratorId, description, questionText, questionHelpText, validationPredicates);
    }

    public PhoneNumberQuestionDefinition(
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

    @JsonDeserialize(
            builder = AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.Builder.class)
    @AutoValue
    public abstract static class PhoneNumberValidationPredicates extends ValidationPredicates {

        public static PhoneNumberValidationPredicates parse(String jsonString) {
            try {
                return mapper.readValue(
                        jsonString, AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public static PhoneNumberValidationPredicates create() {
            return builder().build();
        }
        // Only validation predicate would be that entry contains all numbers/digits
        public static PhoneNumberValidationPredicates create(int minLength, int maxLength) {
            return builder().setMinLength(minLength).setMaxLength(maxLength).build();
        }

        @JsonProperty("minLength")
        public abstract OptionalInt minLength();

        @JsonProperty("maxLength")
        public abstract OptionalInt maxLength();

        public static Builder builder() {
            return new AutoValue_PhoneNumberQuestionDefinition_PhoneNumberValidationPredicates.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {

            @JsonProperty("minLength")
            public abstract Builder setMinLength(OptionalInt minLength);

            public abstract Builder setMinLength(int minLength);

            @JsonProperty("maxLength")
            public abstract Builder setMaxLength(OptionalInt maxLength);

            public abstract Builder setMaxLength(int maxLength);

            public abstract TextValidationPredicates build();
        }
    }

    public PhoneNumberValidationPredicates getPhoneNumberValidationPredicates() {
        return (TextValidationPredicates) getValidationPredicates();
    }

    @Override
    public QuestionType getQuestionType() {
        return QuestionType.TEXT;
    }

    public OptionalInt getMinLength() {
        return getTextValidationPredicates().minLength();
    }

    public OptionalInt getMaxLength() {
        return getTextValidationPredicates().maxLength();
    }
}
