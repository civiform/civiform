package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import models.ApiBridgeConfigurationModel;
import models.BridgeDefinition;
import repository.ApiBridgeRepository;
import services.applicant.ApplicantData;

public class Dispatcher {
  private final IApiBridgeService apiBridgeService;
  private final ApiBridgeRepository apiBridgeRepository;
  private final DispatcherConfigurationService dispatcherConfigurationService;
  private final ApiBridgeExecutionContext apiBridgeExecutionContext;

  private record BridgeContext(
      ApiBridgeConfigurationModel dto, ApiBridgeServiceDto.BridgeResponse bridgeResponse) {}

  @Inject
  public Dispatcher(
      IApiBridgeService apiBridgeService,
      ApiBridgeRepository apiBridgeRepository,
      DispatcherConfigurationService dispatcherConfigurationService,
      ApiBridgeExecutionContext apiBridgeExecutionContext) {
    this.apiBridgeService = checkNotNull(apiBridgeService);
    this.apiBridgeRepository = checkNotNull(apiBridgeRepository);
    this.dispatcherConfigurationService = checkNotNull(dispatcherConfigurationService);
    this.apiBridgeExecutionContext = checkNotNull(apiBridgeExecutionContext);
  }

  public CompletionStage<ApplicantData> dispatchAll(
      ApplicantData applicantData, ImmutableList<BridgeDefinition> bridgeDefinitions) {
    // TODO: GWEN - don't run sequentially
    return apiBridgeRepository
        .getAllEnabled()
        .thenComposeAsync(
            list -> {
              ApplicantData applicantData3 = applicantData;
              for (var item : list) {
                applicantData3 =
                    dispatch(
                            item.getHostUri(),
                            item.getUriPath(),
                            item.getCompatibilityLevel(),
                            applicantData3,
                            bridgeDefinitions.stream()
                                .filter(x -> x.bridgeConfigurationId() == item.getId())
                                .findFirst())
                        .toCompletableFuture()
                        .join();
              }
              return CompletableFuture.completedFuture(applicantData3);
            });
  }

  public CompletionStage<ApplicantData> dispatch(
      String hostUri,
      String uriPath,
      String compatibilityLevel,
      ApplicantData applicantData,
      Optional<BridgeDefinition> bridgeDefinition) {
    return apiBridgeRepository
        .getBridgeConfigurationById(hostUri, uriPath, compatibilityLevel)
        .thenComposeAsync(
            optionalBridgeConfiguration ->
                handleRequest(applicantData, bridgeDefinition, optionalBridgeConfiguration), apiBridgeExecutionContext)
        .thenApplyAsync(ctx -> handleResponse(applicantData, bridgeDefinition, ctx), apiBridgeExecutionContext);
  }

  private CompletionStage<BridgeContext> handleRequest(
      ApplicantData applicantData,
      Optional<BridgeDefinition> optionalBridgeDefinition,
      Optional<ApiBridgeConfigurationModel> optionalBridgeConfiguration) {

    if (optionalBridgeConfiguration.isEmpty()) {
      throw new UnsupportedOperationException("No bridge configuration found");
    }

    var bridgeConfiguration = optionalBridgeConfiguration.get();

    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    BridgeDefinition globalBridgeDefinition;
    try {
      globalBridgeDefinition =
          mapper.readValue(bridgeConfiguration.getGlobalBridgeDefinition(), BridgeDefinition.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    var inputFields =
        optionalBridgeDefinition.isPresent()
            ? optionalBridgeDefinition.get().inputFields()
            : globalBridgeDefinition.inputFields();

    ImmutableMap<String, Object> requestPayloadMap =
        RequestPayloadMapper.map(
            applicantData, bridgeConfiguration.getRequestSchema(), inputFields);

    return apiBridgeService
        .bridge(
            getUri(bridgeConfiguration), new ApiBridgeServiceDto.BridgeRequest(requestPayloadMap))
        .thenApply(bridgeResponse -> new BridgeContext(bridgeConfiguration, bridgeResponse));
  }

  private static ApplicantData handleResponse(
      ApplicantData applicantData, Optional<BridgeDefinition> bridgeDefinition, BridgeContext ctx) {
    //    if (ctx.bridgeResponse().status().statusType() != ApiBridgeServiceDto.StatusType.OK) {
    //      throw new RuntimeException("dispatcher.dispatch not ok");
    //    }

    var newApplicantData = new ApplicantData();
    newApplicantData.mergeFrom(applicantData);

    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    BridgeDefinition globalBridgeDefinition;
    try {
      globalBridgeDefinition =
          mapper.readValue(ctx.dto().getGlobalBridgeDefinition(), BridgeDefinition.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    var outputFields =
        bridgeDefinition.isPresent()
            ? bridgeDefinition.get().outputFields()
            : globalBridgeDefinition.outputFields();

    newApplicantData =
        ResponsePayloadMapper.map(
            newApplicantData,
            ctx.dto.getResponseSchema(),
            ctx.bridgeResponse().payload(),
            outputFields);

    return newApplicantData;
  }

  private static URI getUri(ApiBridgeConfigurationModel cabRepositoryDto) {
    try {
      return new URI(
          String.format("%s%s", cabRepositoryDto.getHostUri(), cabRepositoryDto.getUriPath()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
