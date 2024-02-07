package filters;

import static play.mvc.Results.redirect;

import akka.stream.Materializer;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/** Filter that ensures all sessions have have a unique ID. */

// TODO(#6113): Remove this filter in favor of populating a pac4j profile attribute.
public final class SessionIdFilter extends Filter {
  public static final String SESSION_ID = "sessionId";

  @Inject
  public SessionIdFilter(Materializer mat) {
    super(mat);
  }

  private boolean shouldApplyThisFilter(Http.RequestHeader requestHeader) {
    return NonUserRoutePrefixes.noneMatch(requestHeader)
        // Since we are using redirects, we only apply this filter for a GET request.
        && requestHeader.method().equals("GET")
        && requestHeader.session().get(SESSION_ID).isEmpty();
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (!shouldApplyThisFilter(requestHeader)) {
      return nextFilter.apply(requestHeader);
    }

    // Mint and store a new session id.
    //
    // Since the Play session is immutable for a request, we must redirect in order to get Play to
    // pick up the new value.
    String sessionId = UUID.randomUUID().toString();
    return CompletableFuture.completedFuture(
        redirect(requestHeader.uri())
            .withSession(requestHeader.session().adding(SESSION_ID, sessionId)));
  }
}
