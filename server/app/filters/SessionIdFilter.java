package filters;

import akka.stream.Materializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import akka.util.ByteString;
import com.google.inject.Inject;
import play.core.j.RequestImpl;
import play.libs.streams.Accumulator;
import play.mvc.EssentialAction;
import play.mvc.EssentialFilter;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/** Filter that stores a random session ID in the session, if not already present. */
public final class SessionIdFilter extends EssentialFilter {
  public static final String SESSION_ID = "sessionId";

  private final Executor executor;

  @Inject
  public SessionIdFilter(Executor executor) {
    super();
    this.executor = executor;
  }

//  @Override
//  public CompletionStage<Result> apply(
//      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
//      Http.RequestHeader requestHeader) {
//    return nextFilter
//        .apply(requestHeader)
//        .thenApply(
//            result -> {
//              if (requestHeader.session().get(SESSION_ID).isEmpty()) {
//                String sessionId = UUID.randomUUID().toString();
//                System.out.println("XXX minted sessionId: " + sessionId);
//                return result.withSession(requestHeader.session().adding(SESSION_ID, sessionId));
//              } else {
//                System.out.println("XXX found sessionId: " + requestHeader.session().get(SESSION_ID).get());
//                return result;
//              }
//            });
//  }

  @Override
  public EssentialAction apply(EssentialAction next) {
    return EssentialAction.of(
      request -> {
        Accumulator<ByteString, Result> accumulator = next.apply(request);
        return accumulator.map(
          result -> {
            // XXX NPE below at startup using result.session()
            // XXX request.session() is non-null but causes looping in browser test
//            System.out.println("XXX request.session() = " + request.session());
//            System.out.println("XXX result.session() = " + result.session());
//            System.out.println("XXX request.session().get(SESSION_ID = " + request.session().get(SESSION_ID));
            if (request.getCookie(SESSION_ID).isEmpty()) {
              String sessionId = UUID.randomUUID().toString();
              System.out.println("XXX minted sessionId: " + sessionId);
              return result.withCookies(Http.Cookie.builder(SESSION_ID, sessionId).build());
            } else {
              System.out.println("XXX found sessionId: " + request.getCookie(SESSION_ID).get().value());
              return result;
            }
          },
          executor
        );
      }
    );
  }
}
