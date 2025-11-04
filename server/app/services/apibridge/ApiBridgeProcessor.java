package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.IProblemDetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinition;
import repository.ApiBridgeConfigurationRepository;
import services.ErrorAnd;
import services.JsonUtils;
import services.applicant.ApplicantData;

/** Processor to call all the api bridge endpoints */
@Slf4j
public final class ApiBridgeProcessor {
  private final ApiBridgeService apiBridgeService;
  private final ApiBridgeConfigurationRepository apiBridgeConfigurationRepository;
  private final ApiBridgeExecutionContext apiBridgeExecutionContext;
  private final ObjectMapper mapper;
  private final RequestPayloadMapper requestPayloadMapper;
  private final ResponsePayloadMapper responsePayloadMapper;
  private final SchemaRegistry jsonSchemaFactory =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

  @Inject
  public ApiBridgeProcessor(
      ApiBridgeService apiBridgeService,
      ApiBridgeConfigurationRepository apiBridgeConfigurationRepository,
      ApiBridgeExecutionContext apiBridgeExecutionContext,
      ObjectMapper mapper,
      RequestPayloadMapper requestPayloadMapper,
      ResponsePayloadMapper responsePayloadMapper) {
    this.apiBridgeService = checkNotNull(apiBridgeService);
    this.apiBridgeConfigurationRepository = checkNotNull(apiBridgeConfigurationRepository);
    this.apiBridgeExecutionContext = checkNotNull(apiBridgeExecutionContext);
    this.mapper = checkNotNull(mapper);
    this.requestPayloadMapper = checkNotNull(requestPayloadMapper);
    this.responsePayloadMapper = checkNotNull(responsePayloadMapper);
  }

  /** Calls all configured and enabled api bridge endpoints */
  public CompletionStage<ApplicantData> callApiBridgeEndpoints(
      ApplicantData applicantData, ImmutableMap<String, ApiBridgeDefinition> bridgeDefinitionMap) {
    checkNotNull(applicantData);
    checkNotNull(bridgeDefinitionMap);

    // There are no bridges configured for the program
    if (bridgeDefinitionMap.isEmpty()) {
      return CompletableFuture.completedFuture(applicantData);
    }

    return apiBridgeConfigurationRepository
        .findAllEnabledByAdminNames(bridgeDefinitionMap.keySet())
        .thenComposeAsync(
            bridgeConfigs -> {
              // There are no enabled bridges configured for the program
              if (bridgeConfigs.isEmpty()) {
                return CompletableFuture.completedFuture(applicantData);
              }

              ImmutableList<CompletableFuture<ApplicantData>> stages =
                  bridgeConfigs.stream()
                      .map(
                          bridgeConfig ->
                              callApiBridgeEndpoint(
                                      applicantData,
                                      bridgeDefinitionMap.get(bridgeConfig.adminName()),
                                      bridgeConfig)
                                  .toCompletableFuture())
                      .collect(ImmutableList.toImmutableList());

              return CompletableFuture.allOf(stages.toArray(new CompletableFuture[0]))
                  .thenApplyAsync(
                      v ->
                          stages.stream()
                              .map(CompletableFuture::join)
                              .reduce(
                                  new ApplicantData(),
                                  (accumulator, resultData) -> {
                                    accumulator.mergeFrom(resultData);
                                    return accumulator;
                                  }),
                      apiBridgeExecutionContext);
            });
  }

  /** Calls a single api bridge and handles the result */
  private CompletionStage<ApplicantData> callApiBridgeEndpoint(
      ApplicantData applicantData,
      ApiBridgeDefinition bridgeDefinition,
      ApiBridgeConfigurationModel bridgeConfig) {

    ImmutableMap<String, Object> requestPayload =
        requestPayloadMapper.map(
            applicantData, bridgeConfig.requestSchema(), bridgeDefinition.inputFields());

    // Not at a point in the application where all requisite input fields are populated so we
    // do not attempt to call the api bridge and just return early.
    if (!isRequestValid(requestPayload, bridgeConfig.requestSchema())) {
      return CompletableFuture.completedFuture(applicantData);
    }

    return apiBridgeService
        .bridge(bridgeConfig.getFullHostUrlWithPath(), new BridgeRequest(requestPayload))
        .thenApplyAsync(
            response -> handleResponse(applicantData, bridgeDefinition, bridgeConfig, response),
            apiBridgeExecutionContext);
  }

  /** Handles the api bridge response */
  private ApplicantData handleResponse(
      ApplicantData applicantData,
      ApiBridgeDefinition bridgeDefinition,
      ApiBridgeConfigurationModel bridgeConfig,
      ErrorAnd<ApiBridgeServiceDto.BridgeResponse, IProblemDetail> response) {

    validateResponse(bridgeConfig, response);

    var newApplicantData = new ApplicantData();
    newApplicantData.mergeFrom(applicantData);

    newApplicantData =
        responsePayloadMapper.map(
            newApplicantData,
            bridgeConfig.responseSchema(),
            response.getResult().payload(),
            bridgeDefinition.outputFields());

    return newApplicantData;
  }

  /** Validate payload against the json schema definition */
  private boolean isRequestValid(ImmutableMap<String, Object> payload, String schema) {
    Schema jsonSchema = jsonSchemaFactory.getSchema(schema);
    var validationMessages =
        jsonSchema.validate(JsonUtils.writeValueAsString(mapper, payload), InputFormat.JSON);
    return validationMessages.isEmpty();
  }

  /** Check response for server errors and data validation errors */
  private void validateResponse(
      ApiBridgeConfigurationModel bridgeConfig,
      ErrorAnd<ApiBridgeServiceDto.BridgeResponse, IProblemDetail> response) {
    if (response.isError()) {
      String errorMessage =
          response.getErrors().stream()
              .map(IProblemDetail::asErrorMessage)
              .collect(Collectors.joining("\n"));

      log.error(errorMessage);
      throw new ApiBridgeProcessingException("An error occurred calling endpoint.");
    }

    Schema jsonSchema = jsonSchemaFactory.getSchema(bridgeConfig.responseSchema());
    List<Error> validationMessages =
        jsonSchema.validate(
            JsonUtils.writeValueAsString(mapper, response.getResult().payload()), InputFormat.JSON);

    if (!validationMessages.isEmpty()) {
      String errorMessage =
          validationMessages.stream().map(Error::getMessage).collect(Collectors.joining("\n"));

      log.error(errorMessage);
      throw new ApiBridgeProcessingException("Response payload does not match schema.");
    }
  }
}
