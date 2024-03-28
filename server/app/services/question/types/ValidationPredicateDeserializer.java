package services.question.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class ValidationPredicateDeserializer extends JsonDeserializer<ValidationPredicates> {

    @Override
    public ValidationPredicates deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode root = mapper.readTree(jp);

       System.out.println("root=" + root.toString());

        // TODO
        if (root.has("minLength")) {
            return mapper.readValue(root.toString(), IdQuestionDefinition.IdValidationPredicates.class);
        }
        return mapper.readValue(root.toString(), EmailQuestionDefinition.EmailValidationPredicates.class);
    }
}
