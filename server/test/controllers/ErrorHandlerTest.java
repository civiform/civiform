package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.INTERNAL_SERVER_ERROR;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.admin.NotChangeableException;
import controllers.fileupload.HtmxFileUploadParserErrorHandler;
import java.util.concurrent.CompletionException;
import org.junit.Before;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.i18n.MessagesApi;
import play.mvc.Result;
import repository.ResetPostgres;
import services.apikey.ApiKeyNotFoundException;
import services.program.ProgramNotFoundException;
import views.errors.NotFound;

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
    Result result = handler.onServerError(fakeRequest(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void onServerError_handlesOverride_NotChangeableException() {
    Throwable exception = new NotChangeableException("test exception");
    Result result = handler.onServerError(fakeRequest(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesOverride_ProgramNotFoundException() {
    Throwable exception = new ProgramNotFoundException("test exception");
    Result result = handler.onServerError(fakeRequest(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesBadRequestInsideCompleatableFutureException() {
    Throwable exception = new ProgramNotFoundException("test exception");
    Throwable wrappedException = new CompletionException(exception);
    Result result =
        handler.onServerError(fakeRequest(), wrappedException).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onServerError_handlesCompleatableFutureException() {
    // A non overriden type is handled by the framework still.
    Throwable exception = new RuntimeException("test exception");
    Throwable wrappedException = new CompletionException(exception);
    Result result =
        handler.onServerError(fakeRequest(), wrappedException).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(INTERNAL_SERVER_ERROR);
  }

  @Test
  public void onServerError_handlesOverride_ApiKeyNotFoundException() {
    Throwable exception = new ApiKeyNotFoundException("test exception");
    Result result = handler.onServerError(fakeRequest(), exception).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void onForbidden_csrfMissing_returnsForbiddenWithMessageInDevMode() {
    // Have to manually construct the object here. Unit tests run in
    // test mode, but we only want to have this behavior in dev mode
    Environment devEnv = new Environment(Mode.DEV);

    ErrorHandler devHandler =
        new ErrorHandler(
            instanceOf(Config.class),
            devEnv,
            instanceOf(OptionalSourceMapper.class),
            () -> instanceOf(Router.class),
            () -> instanceOf(NotFound.class),
            instanceOf(MessagesApi.class),
            instanceOf(HtmxFileUploadParserErrorHandler.class));

    Result result =
        devHandler
            .onForbidden(fakeRequest(), "No CSRF token found in body")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(FORBIDDEN);
  }

  @Test
  public void onForbidden_alwaysRedirectsHome() {
    Result result =
        handler
            .onForbidden(fakeRequest(), "No CSRF token found in body")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(play.mvc.Http.Status.SEE_OTHER);

    result =
        handler
            .onForbidden(fakeRequest(), "some other forbidden reason")
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(play.mvc.Http.Status.SEE_OTHER);
  }
}
