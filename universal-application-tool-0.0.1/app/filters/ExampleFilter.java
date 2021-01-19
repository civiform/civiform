package filters;

import java.util.concurrent.Executor;
import javax.inject.Inject;
import javax.inject.Singleton;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;

/** This is a simple filter that adds a header to all requests. */
@Singleton
public class ExampleFilter extends EssentialFilter {

  private final Executor exec;

  /** @param exec This class is needed to execute code asynchronously. */
  @Inject
  public ExampleFilter(Executor exec) {
    this.exec = exec;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request ->
            next.apply(request).map(result -> result.withHeader("X-ExampleFilter", "foo"), exec));
  }
}
