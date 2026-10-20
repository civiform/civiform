package services.email.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.monitoring.EmailSendMetrics;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

public class SimpleEmailTest extends ResetPostgres {
  private SimpleEmail simpleEmail;
  private SesClient sesClient;
  private EmailSendMetrics metrics;

  @Before
  public void setup() {
    simpleEmail = instanceOf(SimpleEmail.class);
    metrics = instanceOf(EmailSendMetrics.class);
    // In the test environment SimpleEmail uses a mock SES client.
    sesClient = simpleEmail.getClient().get();
  }

  @Test
  public void send_success_countsOneSuccessAndNoFailure() {
    double successBefore = statusCount("200");
    double failBefore = metrics.getEmailFailCount().get();

    simpleEmail.send("recipient@example.com", "Subject", "Body");

    assertEquals(successBefore + 1, statusCount("200"), 0);
    assertEquals(failBefore, metrics.getEmailFailCount().get(), 0);
  }

  @Test
  public void send_sesException_countsFailureOnly() {
    doThrow(SesException.builder().statusCode(503).message("service unavailable").build())
        .when(sesClient)
        .sendEmail(any(SendEmailRequest.class));
    double successBefore = statusCount("200");
    double failBefore = metrics.getEmailFailCount().get();
    double status503Before = statusCount("503");

    simpleEmail.send("recipient@example.com", "Subject", "Body");

    assertEquals(successBefore, statusCount("200"), 0);
    assertEquals(failBefore + 1, metrics.getEmailFailCount().get(), 0);
    assertEquals(status503Before + 1, statusCount("503"), 0);
  }

  @Test
  public void send_undeclaredFailure_countsFailureAndRethrows() {
    doThrow(SdkClientException.create("Unable to execute HTTP request"))
        .when(sesClient)
        .sendEmail(any(SendEmailRequest.class));
    double successBefore = statusCount("200");
    double failBefore = metrics.getEmailFailCount().get();
    double unknownBefore = statusCount("unknown");

    assertThrows(
        SdkClientException.class,
        () -> simpleEmail.send("recipient@example.com", "Subject", "Body"));

    assertEquals(successBefore, statusCount("200"), 0);
    assertEquals(failBefore + 1, metrics.getEmailFailCount().get(), 0);
    assertEquals(unknownBefore + 1, statusCount("unknown"), 0);
  }

  private double statusCount(String status) {
    return metrics.getEmailSendCount().labels(status).get();
  }
}
