package modules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.LoggerFactory;
import services.tooling.sql.SqlCaptureAppender;

public class SqlLoggingModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SqlLoggingInitializer.class).asEagerSingleton();
  }

  @Singleton
  public static class SqlLoggingInitializer {

    @Inject
    public SqlLoggingInitializer() {
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

      SqlCaptureAppender appender = new SqlCaptureAppender();
      appender.setContext(context);
      appender.start();

      Logger ebeanSqlLogger = context.getLogger("io.ebean.SQL");

      // If already trace bubble up so it prints to the terminal, otherwise
      // only add to the appender
      ebeanSqlLogger.setAdditive(ebeanSqlLogger.getLevel() == Level.TRACE);
      ebeanSqlLogger.setLevel(ch.qos.logback.classic.Level.TRACE);
      ebeanSqlLogger.addAppender(appender);
    }
  }
}
