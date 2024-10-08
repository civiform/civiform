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
import org.thymeleaf.exceptions.TemplateInputException;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.PlayException;
import play.api.UsefulException;
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
import services.statuses.StatusNotFoundException;
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

  private final Provider<NotFound> notFoundPageProvider;
  private final MessagesApi messagesApi;
  private final Environment environment;
  private final scala.Option<String> playEditor;

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

  private static final ImmutableSet<Class<? extends Exception>> THYMELEAF_EXCEPTION_TYPES =
      ImmutableSet.of(TemplateInputException.class);

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes,
      Provider<NotFound> notFoundPageProvider,
      MessagesApi messagesApi) {
    super(config, environment, sourceMapper, routes);
    this.notFoundPageProvider = checkNotNull(notFoundPageProvider);
    this.messagesApi = checkNotNull(messagesApi);
    this.environment = checkNotNull(environment);

    // Provides extra support to the thymeleaf custom error page if we ever configure it. Standard
    // requirement of the Play error page.
    this.playEditor =
        scala.Option.apply(config.hasPath("play.editor") ? config.getString("play.editor") : null);
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

    match = findThrowableByTypes(exception, THYMELEAF_EXCEPTION_TYPES);

    if (environment.isDev() && match.isPresent()) {
      return CompletableFuture.completedFuture(
          Results.internalServerError(
              views.html.errors.thymeleafDevErrorPage.render(
                  playEditor,
                  new PlayException(
                      "Thymeleaf Compilation Error", exception.getMessage(), exception),
                  request.asScala())));
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

  /**
   * Normally the application handles 403s before this and redirects home. This is a failsafe for
   * edge cases that would leave the user on a grey 403 page that is just builtin to Play. To be
   * consistent with the rest of the app we redirect home.
   *
   * <p>This only known way to hit this is to have an active session, change the application secret,
   * and try using your existing session.
   */
  @Override
  protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
    return CompletableFuture.completedFuture(
        Results.redirect(controllers.routes.HomeController.index().url()));
  }

  @Override
  public CompletionStage<Result> onNotFound(RequestHeader request, String message) {
    return CompletableFuture.completedFuture(
        Results.notFound(
            notFoundPageProvider.get().render(request, messagesApi.preferred(request))));
  }

  @Override
  protected CompletionStage<Result> onProdServerError(
      RequestHeader request, UsefulException exception) {
    return CompletableFuture.completedFuture(
        Results.redirect(
            controllers.routes.InternalServerErrorController.index(exception.id).url()));
  }
}
