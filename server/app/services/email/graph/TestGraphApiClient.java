package services.email.graph;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.mockito.Mockito;

/** Class to use for Graph API unit tests. */
class TestGraphApiClient implements GraphApiClientInterface {
  private final GraphServiceClient graphClient;

  TestGraphApiClient() {
    graphClient = Mockito.mock(GraphServiceClient.class);
  }

  @Override
  public GraphServiceClient get() {
    return graphClient;
  }
}
