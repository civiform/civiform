package services.monitoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

@Singleton
public final class EmailSendMetrics {
  private final Histogram emailExecutionTime;
  private final Counter emailSendCount;
  private final Counter emailFailCount;

  @Inject
  public EmailSendMetrics() {
    emailExecutionTime =
        Histogram.build()
            .name("email_send_time_seconds")
            .help("Execution time of email send")
            .register();

    emailSendCount =
        Counter.build()
            .name("email_send_total")
            .help("Number of emails sent")
            .labelNames("status")
            .register();

    emailFailCount =
        Counter.build()
            .name("email_fail_total")
            .help("Number of emails that failed to send")
            .register();
  }

  public Histogram getEmailExecutionTime() {
    return emailExecutionTime;
  }

  public Counter getEmailSendCount() {
    return emailSendCount;
  }

  public Counter getEmailFailCount() {
    return emailFailCount;
  }
}
