package services.apibridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import models.BridgeDefinitionItem;
import services.Path;
import services.applicant.ApplicantData;

public class ResponsePayloadMapper {

  public static ApplicantData map(
      ApplicantData applicantData,
      String responseSchemaJson,
      ImmutableMap<String, Object> payload,
      ImmutableList<BridgeDefinitionItem> outputFields) {

    JsonNode responseSchema = getJsonNode(responseSchemaJson);

    for (BridgeDefinitionItem outputField : outputFields) {
      String externalDataType =
          responseSchema
              .at(String.format("/properties/%s/type", outputField.externalName()))
              .asText();

      Object value = payload.get(outputField.externalName());
      applicantData = getValue(applicantData, outputField.questionName(), value, externalDataType);
    }

    return applicantData;
  }

  private static ApplicantData getValue(
      ApplicantData applicantData, String questionName, Object value, String externalDataType) {
    Path path = Path.create("applicant").join(questionName);

    switch (externalDataType) {
      case "string" -> applicantData.putString(path, value.toString());
      case "number" -> applicantData.putLong(path, value.toString());
      case "boolean" ->
          applicantData.putString(path, Objects.equals(value.toString(), "true") ? "0" : "1");
    }

    return applicantData;
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
