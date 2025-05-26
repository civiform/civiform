package services.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import services.applicant.ApplicantData;

public class DispatcherConfigurationService {
  public record Config(String uri, ApiBridgeServiceDto.BridgeRequest request) {}

  private final ApiBridgeService apiBridgeService;

  @Inject
  public DispatcherConfigurationService(ApiBridgeService apiBridgeService) {
    this.apiBridgeService = checkNotNull(apiBridgeService);
  }

  public CompletionStage<List<Config>> configurations() {
    return CompletableFuture.completedFuture(new ArrayList<>());
    //    return interlockService
    //        .discovery()
    //        .thenApply(
    //            discoveryResponse ->
    //                Arrays.stream(discoveryResponse.endpoints())
    //                    .filter(x -> x.uri().endsWith("success"))
    //                    .map(
    //                        x -> {
    //                          var uri = String.format("%s%s", BASE_URL, x.uri());
    //
    //                          var request =
    //                              new CabServiceDto.BridgeRequest(ImmutableMap.of());
    //
    //                          return new Config(uri, request);
    //                        })
    //                    .toList());
  }

  public CompletionStage<Config> getConfig(String id, ApplicantData applicantData) {
    return CompletableFuture.completedFuture(new Config("", null));
    //    return interlockService
    //        .discovery()
    //        .thenApply(
    //            discoveryResponse ->
    //                Arrays.stream(discoveryResponse.endpoints())
    //                    .filter(x -> x.uri().endsWith("success"))
    //                    .map(
    //                        x -> {
    //                          String uri = String.format("%s%s", BASE_URL, x.uri());
    //
    //                          var requestParams =
    // ImmutableList.<InterlockServiceDto.AdapterRequestParameter>builder();
    //
    //                          for (var requestParameter : x.requestParameters()) {
    //                            requestParams.add(
    //                              new InterlockServiceDto.AdapterRequestParameter(
    //                                requestParameter.name(),
    //                                "some-request-param-1"
    //                              )
    //                            );
    //                          }
    //
    //                          var request = new InterlockServiceDto.AdapterRequest(
    //
    // requestParams.build().toArray(InterlockServiceDto.AdapterRequestParameter[]::new));
    //
    //                          return new Config(uri, request);
    //                        })
    //                    .findFirst()
    //                    .orElseThrow());
  }
}
