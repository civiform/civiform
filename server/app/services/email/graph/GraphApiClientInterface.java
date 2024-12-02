package services.email.graph;

import com.microsoft.graph.serviceclient.GraphServiceClient;

/** Interface defintion for Graph API client. */
interface GraphApiClientInterface {

  GraphServiceClient get();
}
