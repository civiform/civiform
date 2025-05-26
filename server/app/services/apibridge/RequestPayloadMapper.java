package services.apibridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import models.BridgeDefinitionItem;
import services.Path;
import services.applicant.ApplicantData;

public class RequestPayloadMapper {

  public static ImmutableMap<String, Object> map(
      ApplicantData applicantData,
      String requestSchemaJson,
      ImmutableList<BridgeDefinitionItem> inputFields) {

    JsonNode requestSchema = getJsonNode(requestSchemaJson);
    ImmutableMap.Builder<String, Object> requestPayloadMapBuilder = ImmutableMap.builder();

    inputFields.forEach(
        inputField -> {
          String externalDataType =
              requestSchema
                  .at(String.format("/properties/%s/type", inputField.externalName()))
                  .asText();

          Object value = getValue(applicantData, inputField.questionName(), externalDataType);
          requestPayloadMapBuilder.put(inputField.externalName(), value);
        });

    return requestPayloadMapBuilder.build();
  }

  private static Object getValue(
      ApplicantData applicantData, String questionName, String externalDataType) {
    Path path = Path.create("applicant").join(questionName);

    return switch (externalDataType) {
      case "string" -> applicantData.readString(path).orElse("");
      case "number" -> applicantData.readLong(path).orElse(0L);
      default -> -1;
    };
  }

  private static JsonNode getJsonNode(String jsonString) {
    var mapper = new ObjectMapper().registerModule(new GuavaModule());
    try {
      return mapper.readTree(jsonString);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
