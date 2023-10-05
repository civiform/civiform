package filters;

import akka.stream.Materializer;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.google.inject.Inject;
import play.mvc.Filter;
import play.mvc.Http;
import play.mvc.Result;

/** Filter that stores a random session ID in the session, if not already present. */
public final class SessionIdFilter extends Filter {
  public static final String SESSION_ID = "sessionId";

  @Inject
  public SessionIdFilter(Materializer mat) {
    super(mat);
  }

  @Override
  public CompletionStage<Result> apply(
      Function<Http.RequestHeader, CompletionStage<Result>> nextFilter,
      Http.RequestHeader requestHeader) {
    return nextFilter
        .apply(requestHeader)
        .thenApply(
            result -> {
              if (requestHeader.session().get(SESSION_ID).isEmpty()) {
                String sessionId = UUID.randomUUID().toString();
                System.out.println("XXX minted sessionId: " + sessionId);
                return result.withSession(requestHeader.session().adding(SESSION_ID, sessionId));
              } else {
                System.out.println("XXX found sessionId: " + requestHeader.session().get(SESSION_ID).get());
                return result;
              }
            });
  }

//  @Override
//  public EssentialAction apply(EssentialAction next) {
//    return EssentialAction.of(
//      request -> {
//        Accumulator<ByteString, Result> accumulator = next.apply(request);
//        return accumulator.map(
//          result -> {
//            if (request.session().get(SESSION_ID).isEmpty()) {
//              String sessionId = UUID.randomUUID().toString();
//              System.out.println("XXX minted sessionId: " + sessionId);
//              return result.addingToSession(request, SESSION_ID, sessionId);
//            } else {
//              System.out.println("XXX found sessionId: " + request.session().get(SESSION_ID).get());
//              return result;
//            }
//          },
//          executor
//        );
//      }
//    );
//  }
}
