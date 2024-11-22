package services.email.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import services.cloud.azure.Credentials;

/** Client to use for non-test graph API email sends. */
class GraphApiClient implements GraphApiClientInterface {
  private final GraphServiceClient graphClient;

  GraphApiClient(Credentials credentials) {
    final String[] scopes = new String[] {"https://graph.microsoft.com/.default"};
    graphClient = new GraphServiceClient(checkNotNull(credentials).getCredentials(), scopes);
  }

  @Override
  public GraphServiceClient get() {
    return graphClient;
  }
}
