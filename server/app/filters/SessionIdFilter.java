package filters;

import static play.mvc.Results.redirect;

import akka.stream.Materializer;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/** Filter that stores a random session ID in the session, if not already present. */
public final class SessionIdFilter extends Filter {
  public static final String SESSION_ID = "sessionId";

  public static final ImmutableSet<String> excludedPrefixes = ImmutableSet.of("/api/", "/dev/");

  @Inject
  public SessionIdFilter(Materializer mat) {
    super(mat);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (excludedPrefixes.stream().anyMatch(prefix -> requestHeader.uri().startsWith(prefix))) {
      return nextFilter.apply(requestHeader);
    }
    // If we don't have a session id, mint and store one.
    //
    // Since the Play session is immutable for a request, we must redirect in order to get Play to
    // pick up the new value. Since we are using redirects, we only apply one for a GET request.
    if (requestHeader.session().get(SESSION_ID).isEmpty() && requestHeader.method().equals("GET")) {
      String sessionId = UUID.randomUUID().toString();
      return CompletableFuture.completedFuture(
          redirect(requestHeader.uri())
              .withSession(requestHeader.session().adding(SESSION_ID, sessionId)));
    }
    return nextFilter.apply(requestHeader);
  }
}
