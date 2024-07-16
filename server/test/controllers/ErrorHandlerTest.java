package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static support.FakeRequestBuilder.fakeRequestNew;

import com.google.common.collect.ImmutableSet;
import controllers.admin.NotChangeableException;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import repository.ResetPostgres;
import services.apikey.ApiKeyNotFoundException;
import services.program.ProgramNotFoundException;

public class ErrorHandlerTest extends ResetPostgres {

  private ErrorHandler handler;

  @Before
  public void setUpErrorHandler() {
    handler = instanceOf(ErrorHandler.class);
  }

  @Test
  public void findThrowableByType_findsNone() {
    Throwable exception = new NotChangeableException("test exception");
    assertThat(
            ErrorHandler.findThrowableByTypes(
                exception, ImmutableSet.of(IllegalStateException.class)))
        .isEmpty();
  }

  @Test
  public void findThrowableByType_findsSource() {
    Throwable exception = new NotChangeableException("test exception");
    assertThat(
            ErrorHandler.findThrowableByTypes(
                exception, ImmutableSet.of(NotChangeableException.class)))
        .contains(exception);
  }

  @Test
  public void findThrowableByType_findsNested() {
    Throwable exception = new NotChangeableException("test exception");
    Throwable root =
        new IllegalStateException(
            "level 0", new UnsupportedOperationException("level 1", exception));
    assertThat(
            ErrorHandler.findThrowableByTypes(root, ImmutableSet.of(NotChangeableException.class)))
        .contains(exception);
  }

  @Test
  public void onServerError_defaultBehaviorWorks() {
    // A non overriden type is handled by the framework still.
    Throwable exception = new RuntimeException("test exception");
    Result result = handler.onServerError(fakeRequestNew(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void onServerError_handlesOverride_NotChangeableException() {
    Throwable exception = new NotChangeableException("test exception");
    Result result = handler.onServerError(fakeRequestNew(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesOverride_ProgramNotFoundException() {
    Throwable exception = new ProgramNotFoundException("test exception");
    Result result = handler.onServerError(fakeRequestNew(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesBadRequestInsideCompleatableFutureException() {
    Throwable exception = new ProgramNotFoundException("test exception");
    Throwable wrappedException = new CompletionException(exception);
    Result result =
        handler.onServerError(fakeRequestNew(), wrappedException).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesCompleatableFutureException() {
    // A non overriden type is handled by the framework still.
    Throwable exception = new RuntimeException("test exception");
    Throwable wrappedException = new CompletionException(exception);
    Result result =
        handler.onServerError(fakeRequestNew(), wrappedException).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void onServerError_handlesOverride_ApiKeyNotFoundException() {
    Throwable exception = new ApiKeyNotFoundException("test exception");
    Result result = handler.onServerError(fakeRequestNew(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }
}
