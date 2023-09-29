package filters;

import akka.stream.Materializer;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/** Filter that stores a random session ID in the session, if not already present. */
public final class SessionIdFilter extends Filter {
  public static final String SESSION_ID = "sessionId";

  private Materializer mat;

  @Inject
  public SessionIdFilter(Materializer mat) {
    super(mat);
    this.mat = mat;
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    return nextFilter
        .apply(requestHeader)
        .thenApplyAsync(
            result -> {
              if (requestHeader.session().get(SESSION_ID).isEmpty()) {
                String sessionId = UUID.randomUUID().toString();
                return result.withSession(requestHeader.session().adding(SESSION_ID, sessionId));
              } else {
                return result;
              }
            },
            mat.executionContext());
  }
}
