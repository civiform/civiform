package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Executor;
import javax.inject.Inject;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/**
 * A filter to add the HSTS header to every request. Ensures that all requests to the server have
 * HTTPS enabled for at least the next year - 31536000 seconds.
 */
public class HSTSFilter extends EssentialFilter {
  private final Executor exec;

  @Inject
  public HSTSFilter(Executor exec) {
    this.exec = checkNotNull(exec);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request ->
            next.apply(request)
                .map(
                    // https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security
                    result ->
                        result.withHeader(
                            "Strict-Transport-Security", "max-age=31536000; includeSubDomains"),
                    exec));
  }
}
