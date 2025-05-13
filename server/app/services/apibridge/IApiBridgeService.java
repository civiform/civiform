package services.apibridge;

import static services.apibridge.ApiBridgeServiceDto.BridgeRequest;
import static services.apibridge.ApiBridgeServiceDto.BridgeResponse;
import static services.apibridge.ApiBridgeServiceDto.DiscoveryResponse;
import static services.apibridge.ApiBridgeServiceDto.HealthcheckResponse;

import java.net.URI;
import java.util.concurrent.CompletionStage;

public interface IApiBridgeService {
  CompletionStage<HealthcheckResponse> healthcheck(String hostUri);

  CompletionStage<DiscoveryResponse> discovery(String hostUri);

  CompletionStage<BridgeResponse> bridge(URI uri, BridgeRequest request);
}
