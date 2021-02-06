package filters;

import java.time.Clock;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/** This is a simple filter that produces an apache-style access log. */
@Singleton
public class LoggingFilter extends EssentialFilter {

  private final Executor exec;
  private final Clock clock;
  private static final Logger log = LoggerFactory.getLogger("loggingfilter");

  /** @param exec This class is needed to execute code asynchronously. */
  @Inject
  public LoggingFilter(Executor exec, Clock clock) {
    this.exec = exec;
    this.clock = clock;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          final long startTime = clock.millis();
          return next.apply(request)
              .map(
                  result -> {
                    long time = clock.millis() - startTime;
                    log.info(
                        "{}\t{}\t{}ms\t{}", request.method(), request.uri(), time, result.status());
                    return result.withHeader("X-RequestTime", String.valueOf(time));
                  },
                  exec);
        });
  }
}
