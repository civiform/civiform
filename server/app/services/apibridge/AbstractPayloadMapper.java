package services.apibridge;

import com.fasterxml.jackson.databind.JsonNode;
import services.apibridge.ApiBridgeServiceDto.JsonSchemaDataType;
import services.question.types.ScalarType;

/** Common base with shared methods used by Api Bridge mappers */
public abstract class AbstractPayloadMapper {
  protected record TypePair(
      ApiBridgeServiceDto.JsonSchemaDataType jsonType, ScalarType scalarType) {}

  /** Determines if the {@link ScalarType} is supported or not */
  protected boolean isUnsupportedScalarType(ScalarType scalarType) {
    return switch (scalarType) {
      case CURRENCY_CENTS, DATE, DOUBLE, LONG, STRING, PHONE_NUMBER -> false;
      case LIST_OF_STRINGS, SERVICE_AREA -> true;
    };
  }

  /** Get the {@link JsonSchemaDataType} based on the requested external question data type */
  protected JsonSchemaDataType getJsonSchemaDataType(JsonNode jsonNode, String externalName) {
    String externalDataTypeName =
        jsonNode.at(String.format("/properties/%s/type", externalName)).asText();

    return JsonSchemaDataType.fromValue(externalDataTypeName);
  }
}
