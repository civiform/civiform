package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinitionItem;
import org.apache.commons.lang3.function.TriConsumer;
import services.JsonUtils;
import services.Path;
import services.apibridge.ApiBridgeServiceDto.JsonSchemaDataType;
import services.applicant.ApplicantData;
import services.question.YesNoQuestionOption;
import services.question.types.ScalarType;

/** Handles mapping the response from the Api Bridge to {@link ApplicantData} */
@Slf4j
public final class ResponsePayloadMapper extends AbstractPayloadMapper {
  private final ObjectMapper mapper;

  // This creates a mapping between internal/external data types and the appropriate
  // put method on the ApplicantData.
  private final ImmutableMap<TypePair, TriConsumer<ApplicantData, Path, String>>
      TYPEPAIR_WRITER_MAP =
          ImmutableMap.of(
              new TypePair(JsonSchemaDataType.STRING, ScalarType.STRING),
              ApplicantData::putString,
              new TypePair(JsonSchemaDataType.STRING, ScalarType.PHONE_NUMBER),
              ApplicantData::putPhoneNumber,
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.LONG),
              ApplicantData::putLong,
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.DOUBLE),
              ApplicantData::putDouble,
              new TypePair(JsonSchemaDataType.STRING, ScalarType.DATE),
              ApplicantData::putDate,
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.CURRENCY_CENTS),
              ApplicantData::putCurrencyDollars,
              new TypePair(JsonSchemaDataType.BOOLEAN, ScalarType.STRING),
              (applicantData, path, value) ->
                  applicantData.putLong(
                      path,
                      Long.toString(
                          YesNoQuestionOption.fromBoolean(Boolean.parseBoolean(value)).getId())));

  @Inject
  public ResponsePayloadMapper(ObjectMapper mapper) {
    this.mapper = checkNotNull(mapper);
  }

  /** Map requested output fields from the response payload to the {@link ApplicantData}. */
  public ApplicantData map(
      ApplicantData applicantData,
      String responseSchemaJson,
      ImmutableMap<String, Object> payload,
      ImmutableList<ApiBridgeDefinitionItem> outputFields) {

    JsonNode responseSchema = JsonUtils.readTree(mapper, responseSchemaJson);

    for (ApiBridgeDefinitionItem outputField : outputFields) {
      if (isUnsupportedScalarType(outputField.questionScalar().toScalarType())) {
        throw new RuntimeException(
            String.format(
                "Unsupported question type. Question '%s' of type '%s' bound to external '%s'.",
                outputField.questionName(),
                outputField.questionScalar().toDisplayString(),
                outputField.externalName()));
      }

      JsonSchemaDataType jsonSchemaDataType =
          getJsonSchemaDataType(responseSchema, outputField.externalName());

      switch (jsonSchemaDataType) {
        case BOOLEAN, NUMBER, STRING -> {
          Object value = payload.get(outputField.externalName());
          setValue(applicantData, outputField, value, jsonSchemaDataType);
        }
        default ->
            log.info(
                "JsonSchema property of '{}' is not supported and will be skipped.",
                jsonSchemaDataType);
      }
    }

    return applicantData;
  }

  /**
   * Set the value for {@link ApplicantData} from the response based on the source and destination
   * pairing.
   */
  private void setValue(
      ApplicantData applicantData,
      ApiBridgeDefinitionItem bridgeDefinitionItem,
      Object value,
      JsonSchemaDataType jsonSchemaDataType) {
    TypePair typePair =
        new TypePair(jsonSchemaDataType, bridgeDefinitionItem.questionScalar().toScalarType());
    TriConsumer<ApplicantData, Path, String> writerFunction = TYPEPAIR_WRITER_MAP.get(typePair);

    if (writerFunction == null) {
      log.warn(
          "No writer map for type pairing [{},{}]", typePair.jsonType(), typePair.scalarType());
      return;
    }

    Path path =
        ApplicantData.APPLICANT_PATH
            .join(bridgeDefinitionItem.questionName())
            .join(bridgeDefinitionItem.questionScalar());

    writerFunction.accept(applicantData, path, value.toString());
  }
}
