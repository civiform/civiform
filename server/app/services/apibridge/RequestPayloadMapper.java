package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinitionItem;
import services.JsonUtils;
import services.Path;
import services.apibridge.ApiBridgeServiceDto.JsonSchemaDataType;
import services.applicant.ApplicantData;
import services.applicant.Currency;
import services.question.YesNoQuestionOption;
import services.question.types.ScalarType;

/** Handles mapping {@link ApplicantData} to a format that can be sent to the Api Bridge. */
@Slf4j
public final class RequestPayloadMapper extends AbstractPayloadMapper {
  private final ObjectMapper mapper;

  // This creates a mapping between internal/external data types and the appropriate
  // read method on the ApplicantData.
  private final ImmutableMap<TypePair, BiFunction<ApplicantData, Path, Optional<?>>>
      TYPEPAIR_READER_MAP =
          ImmutableMap.of(
              new TypePair(JsonSchemaDataType.STRING, ScalarType.STRING),
              ApplicantData::readString,
              new TypePair(JsonSchemaDataType.STRING, ScalarType.PHONE_NUMBER),
              ApplicantData::readString,
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.LONG),
              ApplicantData::readLong,
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.DOUBLE),
              ApplicantData::readDouble,
              new TypePair(JsonSchemaDataType.STRING, ScalarType.DATE),
              (applicantData, path) ->
                  applicantData.readDate(path).map(x -> x.format(DateTimeFormatter.ISO_LOCAL_DATE)),
              new TypePair(JsonSchemaDataType.NUMBER, ScalarType.CURRENCY_CENTS),
              (applicantData, path) -> applicantData.readCurrency(path).map(Currency::getDollars),
              new TypePair(JsonSchemaDataType.BOOLEAN, ScalarType.STRING),
              (applicantData, path) ->
                  applicantData
                      .readLong(path)
                      .flatMap(x -> YesNoQuestionOption.fromId(x).toOptionalBoolean()));

  @Inject
  public RequestPayloadMapper(ObjectMapper mapper) {
    this.mapper = checkNotNull(mapper);
  }

  /** Map requested input fields from {@link ApplicantData} to the request payload. */
  public ImmutableMap<String, Object> map(
      ApplicantData applicantData,
      String requestSchemaJson,
      ImmutableList<ApiBridgeDefinitionItem> inputFields) {

    JsonNode requestSchema = JsonUtils.readTree(mapper, requestSchemaJson);
    ImmutableMap.Builder<String, Object> requestPayloadMapBuilder = ImmutableMap.builder();

    inputFields.forEach(
        inputField -> {
          if (isUnsupportedScalarType(inputField.questionScalar().toScalarType())) {
            throw new RuntimeException(
                String.format(
                    "Unsupported question type. Question '%s' of type '%s' bound to external '%s'.",
                    inputField.questionName(),
                    inputField.questionScalar().toDisplayString(),
                    inputField.externalName()));
          }

          JsonSchemaDataType jsonSchemaDataType =
              getJsonSchemaDataType(requestSchema, inputField.externalName());

          switch (jsonSchemaDataType) {
            case BOOLEAN, NUMBER, STRING -> {
              Optional<?> optionalValue = getValue(applicantData, inputField, jsonSchemaDataType);
              optionalValue.ifPresent(
                  v -> requestPayloadMapBuilder.put(inputField.externalName(), v));
            }
            default ->
                log.info(
                    "JsonSchema property of '{}' is not supported and will be skipped.",
                    jsonSchemaDataType);
          }
        });

    return requestPayloadMapBuilder.build();
  }

  /**
   * Get the value for the request from the {@link ApplicantData} based on the source and
   * destination pairing.
   */
  private Optional<?> getValue(
      ApplicantData applicantData,
      ApiBridgeDefinitionItem bridgeDefinitionItem,
      JsonSchemaDataType jsonSchemaDataType) {
    TypePair typePair =
        new TypePair(jsonSchemaDataType, bridgeDefinitionItem.questionScalar().toScalarType());
    BiFunction<ApplicantData, Path, Optional<?>> readerFunction = TYPEPAIR_READER_MAP.get(typePair);

    if (readerFunction == null) {
      log.warn(
          "No reader map for type pairing [{},{}]", typePair.jsonType(), typePair.scalarType());
      return Optional.empty();
    }

    Path path =
        ApplicantData.APPLICANT_PATH
            .join(bridgeDefinitionItem.questionName())
            .join(bridgeDefinitionItem.questionScalar());

    return readerFunction.apply(applicantData, path);
  }
}
