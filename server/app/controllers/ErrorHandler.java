package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.UnauthorizedApiRequestException;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.admin.NotChangeableException;
import controllers.api.BadApiRequestException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.i18n.MessagesApi;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;
import services.apikey.ApiKeyNotFoundException;
import services.applicant.exception.ApplicantNotFoundException;
import services.applications.AccountHasNoEmailException;
import services.applications.StatusEmailNotFoundException;
import services.program.InvalidQuestionPositionException;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionInvalidException;
import services.program.StatusNotFoundException;
import views.errors.NotFound;

/**
 * Override for the system default {@code HttpErrorHandler}.
 *
 * <p>This lets us do things like throw RuntimeExceptions in the application but then surface them
 * as 400 level responses to the user.
 *
 * <p>https://www.playframework.com/documentation/2.8.x/JavaErrorHandling#Extending-the-default-error-handler
 */
@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

  private final NotFound notFoundPage;
  private final MessagesApi messagesApi;

  private static final ImmutableSet<Class<? extends Exception>> BAD_REQUEST_EXCEPTION_TYPES =
      ImmutableSet.of(
          AccountHasNoEmailException.class,
          ApiKeyNotFoundException.class,
          ApplicantNotFoundException.class,
          BadApiRequestException.class,
          BadRequestException.class,
          InvalidQuestionPositionException.class,
          NotChangeableException.class,
          ProgramNotFoundException.class,
          ProgramQuestionDefinitionInvalidException.class,
          StatusEmailNotFoundException.class,
          StatusNotFoundException.class);

  private static final ImmutableSet<Class<? extends Exception>>
      UNAUTHORIZED_REQUEST_EXCEPTION_TYPES = ImmutableSet.of(UnauthorizedApiRequestException.class);

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes,
      NotFound notFoundPage,
      MessagesApi messagesApi) {
    super(config, environment, sourceMapper, routes);
    this.notFoundPage = checkNotNull(notFoundPage);
    this.messagesApi = checkNotNull(messagesApi);
  }

  @Override
  public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {

    // Unwrap exceptions thrown within a CompletableFuture, to handle the
    // original error stack trace.
    if (exception instanceof CompletionException) {
      exception = exception.getCause();
    }

    // Exceptions that reach here will generate 500s. Here we convert certain ones to different user
    // visible states. Note: there are methods on the parent that handle dev and prod separately.
    Optional<Throwable> match = findThrowableByTypes(exception, BAD_REQUEST_EXCEPTION_TYPES);

    if (match.isPresent()) {
      return CompletableFuture.completedFuture(Results.badRequest(match.get().getMessage()));
    }

    match = findThrowableByTypes(exception, UNAUTHORIZED_REQUEST_EXCEPTION_TYPES);

    if (match.isPresent()) {
      return CompletableFuture.completedFuture(Results.unauthorized());
    }

    return super.onServerError(request, exception);
  }

  /**
   * Finds an exception of type {@code search} by looking through {@code exception}'s cause chain a
   * few levels deep. Will also consider {@code exception}.
   *
   * <p>The framework provides wrapped exceptions to the methods in this class so we have to dig out
   * our application exception. Anecdotally it's 2 levels down.
   */
  static Optional<Throwable> findThrowableByTypes(
      Throwable exception, ImmutableSet<Class<? extends Exception>> search) {
    Optional<Throwable> root = Optional.of(exception);
    // Search a couple causes deep for the desired type.
    for (int i = 0; i < 5 && root.isPresent(); ++i) {
      if (search.contains(root.get().getClass())) {
        return root;
      }
      root = Optional.ofNullable(root.get().getCause());
    }
    return Optional.empty();
  }

  @Override
  public CompletionStage<Result> onNotFound(RequestHeader request, String message) {
    return CompletableFuture.completedFuture(
        Results.notFound(notFoundPage.render(request, messagesApi.preferred(request))));
  }
}
