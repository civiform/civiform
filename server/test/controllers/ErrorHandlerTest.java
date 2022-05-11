package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static play.test.Helpers.fakeRequest;

import controllers.admin.NotChangeableException;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import repository.ResetPostgres;

public class ErrorHandlerTest extends ResetPostgres {

  private ErrorHandler handler;

  @Before
  public void setUpErrorHandler() {
    handler = instanceOf(ErrorHandler.class);
  }

  @Test
  public void findThrowableByType_findsNone() {
    Throwable exception = new NotChangeableException("test exception");
    assertThat(ErrorHandler.findThrowableByType(exception, IllegalStateException.class)).isEmpty();
  }

  @Test
  public void findThrowableByType_findsSource() {
    Throwable exception = new NotChangeableException("test exception");
    assertThat(ErrorHandler.findThrowableByType(exception, RuntimeException.class))
        .contains(exception);
  }

  @Test
  public void findThrowableByType_findsNested() {
    Throwable exception = new NotChangeableException("test exception");
    Throwable root =
        new IllegalStateException(
            "level 0", new UnsupportedOperationException("level 1", exception));
    assertThat(ErrorHandler.findThrowableByType(root, NotChangeableException.class))
        .contains(exception);
  }

  @Test
  public void onServerError_defaultBehaviorWorks() {
    // A non overriden type is handled by the framework still.
    Throwable exception = new RuntimeException("test exception");
    Result result =
        handler.onServerError(fakeRequest().build(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void onServerError_handlesOverride() {
    Throwable exception = new NotChangeableException("test exception");
    Result result =
        handler.onServerError(fakeRequest().build(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }
}
