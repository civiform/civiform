package services.tooling.sql;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class SqlCaptureAppender extends AppenderBase<ILoggingEvent> {
  @Override
  protected void append(ILoggingEvent event) {
    RequestSqlCollector.add(event.getFormattedMessage());
  }
}
