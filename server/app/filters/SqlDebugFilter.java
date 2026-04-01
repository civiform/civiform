package filters;

import javax.inject.Inject;
import org.apache.pekko.stream.Materializer;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import services.tooling.sql.RequestSqlCollector;

public class SqlDebugFilter extends EssentialFilter {

  private final Materializer materializer;

  @Inject
  public SqlDebugFilter(Materializer materializer) {
    this.materializer = materializer;
  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
        request -> {
          //      if (request.queryString("show-debug-tools").filter(values ->
          // values.contains("true")).isEmpty()) {
          //        return next.apply(request);
          //      }

          RequestSqlCollector.start();
          return next.apply(request)
              .map(
                  result -> {
                    RequestSqlCollector.end();
                    return result;
                  },
                  materializer.executionContext());
        });
  }
}
