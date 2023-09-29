package filters;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import akka.stream.Materializer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

public class SessionIdFilterTest extends WithApplication {
  @Test
  public void testSessionIdIsCreated() throws Exception {
    Materializer mat = app.injector().instanceOf(Materializer.class);
    SessionIdFilter filter = new SessionIdFilter(mat);

    // The request has no session id.
    Http.RequestBuilder request = fakeRequest();
    assertThat(request.session().containsKey(SessionIdFilter.SESSION_ID)).isFalse();

    CompletionStage<Result> stage =
        filter.apply(
            header -> {
              return CompletableFuture.completedFuture(play.mvc.Results.ok());
            },
            request.build());

    Result result = stage.toCompletableFuture().get();
    assertThat(result.session().get(SessionIdFilter.SESSION_ID)).isNotEmpty();
  }
}
