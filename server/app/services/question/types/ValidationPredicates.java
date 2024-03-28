package services.question.types;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;


  @JsonTypeInfo(
          use = JsonTypeInfo.Id.NAME,
         // include = JsonTypeInfo.As.EXISTING_PROPERTY,
          property = "type")
  @JsonSubTypes({

          @JsonSubTypes.Type(value = AutoValue_AddressQuestionDefinition_AddressValidationPredicates.class, name = "address"),
          @JsonSubTypes.Type(value = AutoValue_CurrencyQuestionDefinition_CurrencyValidationPredicates.class, name = "currency"),

          @JsonSubTypes.Type(value = AutoValue_DateQuestionDefinition_DateValidationPredicates.class, name = "date"),
          @JsonSubTypes.Type(value = AutoValue_EmailQuestionDefinition_EmailValidationPredicates.class, name = "email"),
          @JsonSubTypes.Type(value = AutoValue_EnumeratorQuestionDefinition_EnumeratorValidationPredicates.class, name = "enumerator"),
          @JsonSubTypes.Type(value = AutoValue_FileUploadQuestionDefinition_FileUploadValidationPredicates.class, name = "fileupload"),
          @JsonSubTypes.Type(value = AutoValue_IdQuestionDefinition_IdValidationPredicates.class, name = "id"),
          @JsonSubTypes.Type(value = AutoValue_MultiOptionQuestionDefinition_MultiOptionValidationPredicates.class, name = "multioption"),
          @JsonSubTypes.Type(value = NameQuestionDefinition.NameValidationPredicates.class, name = "name"),
          @JsonSubTypes.Type(value = AutoValue_NumberQuestionDefinition_NumberValidationPredicates.class, name = "number"),
          @JsonSubTypes.Type(value = AutoValue_PhoneQuestionDefinition_PhoneValidationPredicates.class, name = "phone"),
          @JsonSubTypes.Type(value = AutoValue_StaticContentQuestionDefinition_StaticContentValidationPredicates.class, name = "static"),
          @JsonSubTypes.Type(value = AutoValue_TextQuestionDefinition_TextValidationPredicates.class, name = "text"),

  })
// TODO: NEed to comment out whenever not using the Import tab because not all question predicates
// are handled in the Deserializer
// @JsonDeserialize(using = ValidationPredicateDeserializer.class)
public abstract class ValidationPredicates {
    protected static final ObjectMapper mapper =
            new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public String serializeAsString() {
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
