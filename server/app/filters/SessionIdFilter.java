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

/** Filter that ensures all sessions have have a unique ID. */
public final class SessionIdFilter extends Filter {
  public static final String SESSION_ID = "sessionId";

  public static final ImmutableSet<String> excludedPrefixes =
      ImmutableSet.of("/api/", "/dev/", "/favicon");

  @Inject
  public SessionIdFilter(Materializer mat) {
    super(mat);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    if (excludedPrefixes.stream().anyMatch(prefix -> requestHeader.uri().startsWith(prefix)) ||
        requestHeader.session().get(SESSION_ID).isPresent() ||
        !requestHeader.method().equals("GET")) {
      return nextFilter.apply(requestHeader);
    }
    
    // Mint and store one a new session id.
    //
    // Since the Play session is immutable for a request, we must redirect in order to get Play to
    // pick up the new value. Since we are using redirects, we only apply one for a GET request.
      String sessionId = UUID.randomUUID().toString();
      return CompletableFuture.completedFuture(
          redirect(requestHeader.uri())
              .withSession(requestHeader.session().adding(SESSION_ID, sessionId)));
  }
}
