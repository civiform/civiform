package filters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Executor;
import javax.inject.Inject;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/** A filter to disable caching our responses. */
public class DisableCachingFilter extends EssentialFilter {
  private final Executor exec;

  @Inject
  public DisableCachingFilter(Executor exec) {
    this.exec = checkNotNull(exec);
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request ->
            next.apply(request)
                .map(
                    result ->
                        result
                            .withHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .withHeader("Pragma", "no-cache")
                            .withHeader("Expires", "0"),
                    exec));
  }
}
