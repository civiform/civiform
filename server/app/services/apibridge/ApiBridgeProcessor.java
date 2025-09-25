package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.IProblemDetail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinition;
import repository.ApiBridgeConfigurationRepository;
import services.ErrorAnd;
import services.applicant.ApplicantData;

/** Processor to call all the api bridge endpoints */
@Slf4j
public final class ApiBridgeProcessor {
  private final ApiBridgeService apiBridgeService;
  private final ApiBridgeConfigurationRepository apiBridgeConfigurationRepository;
  private final ApiBridgeExecutionContext apiBridgeExecutionContext;
  private final RequestPayloadMapper requestPayloadMapper;
  private final ResponsePayloadMapper responsePayloadMapper;

  @Inject
  public ApiBridgeProcessor(
      ApiBridgeService apiBridgeService,
      ApiBridgeConfigurationRepository apiBridgeConfigurationRepository,
      ApiBridgeExecutionContext apiBridgeExecutionContext,
      RequestPayloadMapper requestPayloadMapper,
      ResponsePayloadMapper responsePayloadMapper) {
    this.apiBridgeService = checkNotNull(apiBridgeService);
    this.apiBridgeConfigurationRepository = checkNotNull(apiBridgeConfigurationRepository);
    this.apiBridgeExecutionContext = checkNotNull(apiBridgeExecutionContext);
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
    return apiBridgeService
        .bridge(
            bridgeConfig.getFullHostUrlWithPath(),
            new BridgeRequest(
                requestPayloadMapper.map(
                    applicantData, bridgeConfig.requestSchema(), bridgeDefinition.inputFields())))
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
    if (response.isError()) {
      String errorMessage =
          response.getErrors().stream()
              .map(IProblemDetail::asErrorMessage)
              .collect(Collectors.joining("\n"));

      log.error(errorMessage);
      throw new ApiBridgeProcessingException("An error occurred calling. " + errorMessage);
    }

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
}
