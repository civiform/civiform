package controllers;

import play.api.Environment;
import play.api.OptionalSourceMapper;
import play.api.Configuration;
import play.api.routing.Router;
import javax.inject.Provider;

import static com.google.common.base.Preconditions.checkNotNull;
import javax.inject.Singleton;
import javax.inject.Inject;
import auth.ProfileUtils;
import play.libs.concurrent.HttpExecutionContext;
import play.api.http.DefaultHttpErrorHandler;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.i18n.MessagesApi;
import views.NotFoundPage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import views.errors.NotFound;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {
  private final NotFound notFoundPage;
  private final MessagesApi messagesApi;

  @Inject
  public ErrorHandler(
      Configuration config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes,
      NotFound notFoundPage, MessagesApi messagesApi) {
    super(environment, config, sourceMapper, routes);
    this.notFoundPage=notFoundPage;
    this.messagesApi=messagesApi;
  }

  protected CompletionStage<Result> onNotFound(Http.RequestHeader requestHeader, String message) {
    return CompletableFuture.completedFuture(
        Results.forbidden("Page not found. Check URL is correct"));
        /*Results.notFound(notFoundPage.render(messagesApi.preferred(requestHeader))));*/
  }
}
