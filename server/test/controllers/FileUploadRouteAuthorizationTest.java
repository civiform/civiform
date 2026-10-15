package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.util.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import parsers.StreamingMultipartUploadResult;
import parsers.cloud.MultipartUploadSinks;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.test.Helpers;
import play.test.TestServer;
import services.cloud.StorageServiceName;

/**
 * The streaming upload body parsers write a file to cloud storage as the request body is read. Play
 * runs the body parser before action composition unless the route defers body parsing, so by
 * default an {@code @Secure} authorizer only runs after the file is stored.
 *
 * <p>These tests send real HTTP requests with no session to a running test server, because body
 * parsing deferral is applied by the HTTP server and is skipped when a request is routed directly
 * with {@code Helpers.route}. Each test checks that the request is turned away and that the upload
 * sink is never created, which means the checks ran before any bytes were read.
 */
public class FileUploadRouteAuthorizationTest {

  // A small PNG header, long enough to pass the parser's file type sniffing.
  private static final byte[] PNG_BYTES = pngBytes();

  // A minimal PDF with enough bytes to pass file type sniffing.
  private static final byte[] PDF_BYTES = pdfBytes();

  private MultipartUploadSinks uploadSinks;
  private Application app;
  private TestServer server;

  @Before
  public void setUp() {
    uploadSinks = mock(MultipartUploadSinks.class);
    // If a parser ever asks for a sink, hand back one that works so the request completes and the
    // assertion below, not a stream error, is what fails.
    when(uploadSinks.getSinkForCloudProvider(any(), anyString(), anyInt()))
        .thenAnswer(
            _ ->
                Sink.<ByteString>ignore()
                    .mapMaterializedValue(
                        done ->
                            done.thenApply(
                                _ ->
                                    StreamingMultipartUploadResult.builder()
                                        .setStatus(StreamingMultipartUploadResult.Status.SUCCESS)
                                        .setStorageServiceName(StorageServiceName.S3)
                                        .build())));
    app =
        new GuiceApplicationBuilder()
            .overrides(bind(MultipartUploadSinks.class).toInstance(uploadSinks))
            .build();
    server = Helpers.testServer(app);
    server.start();
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void applicantUpload_noSession_startsNoUpload() throws Exception {
    assertNoUploadStarted(
        "/programs/1/blocks/1/hx/selectFileForUpload", "doc.pdf", "application/pdf", PDF_BYTES);
  }

  @Test
  public void applicantUploadWithApplicantId_noSession_startsNoUpload() throws Exception {
    assertNoUploadStarted(
        "/applicants/1/programs/1/blocks/1/hx/selectFileForUpload",
        "doc.pdf",
        "application/pdf",
        PDF_BYTES);
  }

  @Test
  public void programImageUpload_noSession_startsNoUpload() throws Exception {
    assertNoUploadStarted(
        "/admin/programs/1/image/upload/CREATION", "image.png", "image/png", PNG_BYTES);
  }

  @Test
  public void questionImageUpload_noSession_startsNoUpload() throws Exception {
    assertNoUploadStarted(
        "/admin/questions/1/hx/image/upload", "image.png", "image/png", PNG_BYTES);
  }

  private void assertNoUploadStarted(
      String path, String fileName, String contentType, byte[] content) throws Exception {
    String boundary = "----cfTestBoundary7MA4YWxkTrZu0gW";
    URI uri = URI.create("http://localhost:" + server.getRunningHttpPort().getAsInt() + path);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(
                HttpRequest.BodyPublishers.ofByteArray(
                    multipartBody(boundary, fileName, contentType, content)))
            .build();

    // Do not follow redirects, so a login redirect shows up as a 303.
    HttpResponse<String> response =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());

    // An unauthenticated request gets the login redirect, the same as it did before deferral, and
    // must not have started writing the file anywhere.
    assertThat(response.statusCode()).isEqualTo(Http.Status.SEE_OTHER);
    verifyNoInteractions(uploadSinks);
  }

  private static byte[] multipartBody(
      String boundary, String fileName, String contentType, byte[] content) throws IOException {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    String head =
        "--"
            + boundary
            + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
            + fileName
            + "\"\r\nContent-Type: "
            + contentType
            + "\r\n\r\n";
    body.write(head.getBytes(StandardCharsets.US_ASCII));
    body.write(content);
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
    return body.toByteArray();
  }

  private static byte[] pngBytes() {
    byte[] header = {
      (byte) 0x89,
      'P',
      'N',
      'G',
      '\r',
      '\n',
      0x1a,
      '\n',
      0,
      0,
      0,
      13,
      'I',
      'H',
      'D',
      'R',
      0,
      0,
      0,
      1,
      0,
      0,
      0,
      1,
      8,
      2,
      0,
      0,
      0
    };
    return Arrays.copyOf(header, 64);
  }

  private static byte[] pdfBytes() {
    StringBuilder pdf = new StringBuilder("%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\n");
    while (pdf.length() < 512) {
      pdf.append("%padding\n");
    }
    pdf.append("%%EOF\n");
    return pdf.toString().getBytes(StandardCharsets.US_ASCII);
  }
}
