package support;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

/**
 * This is a decorator for {@link WSClient} that allows for customizing the HTTP headers before
 * having to commit to calling the URL.
 */
public class WSClientWithHttpHeader implements WSClient {
  private final WSClient wsClient;
  private final Map<String, List<String>> headers = new HashMap<>();

  public WSClientWithHttpHeader(WSClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public Object getUnderlying() {
    return wsClient.getUnderlying();
  }

  @Override
  public play.api.libs.ws.WSClient asScala() {
    return wsClient.asScala();
  }

  @Override
  public WSRequest url(String url) {
    return wsClient.url(url).setHeaders(headers);
  }

  @Override
  public void close() throws IOException {
    wsClient.close();
  }

  /** Custom headers to be added to the HTTP call */
  public WSClientWithHttpHeader setHeaders(Map<String, List<String>> headers) {
    this.headers.clear();
    this.headers.putAll(headers);
    return this;
  }
}
