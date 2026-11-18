package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.test.TestServer;
import services.DeploymentType;

/**
 * Sends raw HTTP requests to a running server that is neither dev nor staging, to check that {@link
 * DevRoutesFilter} blocks /dev routes however the request target is written.
 *
 * <p>Fake requests can't cover this: they don't go through the HTTP parser, so they can't send an
 * absolute-form request target.
 */
public class DevRoutesFilterServerTest {

  private static TestServer server;
  private static int port;

  @BeforeClass
  public static void startServer() {
    server = new TestServer(/* port= */ 0, fakeApplication());
    server.start();
    port = server.getRunningHttpPort().orElseThrow();
  }

  @AfterClass
  public static void stopServer() {
    server.stop();
  }

  @Test
  public void testApp_isNotDevOrStaging() {
    assertThat(server.application().injector().instanceOf(DeploymentType.class).isDevOrStaging())
        .isFalse();
  }

  @Test
  public void devRoute_originForm_returnsNotFound() throws IOException {
    assertThat(statusLine("GET /dev/seed HTTP/1.1")).contains(" 404 ");
  }

  @Test
  public void devRoute_absoluteForm_returnsNotFound() throws IOException {
    assertThat(statusLine("GET http://localhost/dev/seed HTTP/1.1")).contains(" 404 ");
  }

  @Test
  public void nonDevRoute_absoluteForm_isRouted() throws IOException {
    // Guards the test above: the server must accept absolute-form targets and route them, or a
    // 404 there wouldn't show that the filter did anything.
    assertThat(statusLine("GET http://localhost/ HTTP/1.1")).doesNotContain(" 404 ");
  }

  private static String statusLine(String requestLine) throws IOException {
    try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
      OutputStream out = socket.getOutputStream();
      out.write(
          (requestLine + "\r\nHost: localhost\r\nConnection: close\r\n\r\n")
              .getBytes(StandardCharsets.US_ASCII));
      out.flush();
      BufferedReader in =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
      return in.readLine();
    }
  }
}
