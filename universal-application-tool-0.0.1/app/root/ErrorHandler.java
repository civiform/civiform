package controllers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import play.api.Configuration;
import play.api.Environment;
import play.api.OptionalSourceMapper;
import play.api.http.DefaultHttpErrorHandler;
import play.api.routing.Router;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
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
      NotFound notFoundPage,
      MessagesApi messagesApi) {
    super(environment, config, sourceMapper, routes);
    this.notFoundPage = notFoundPage;
    this.messagesApi = messagesApi;
  }

  /*protected CompletionStage<Result> onNotFound(Http.RequestHeader requestHeader, String message) {
    return CompletableFuture.completedFuture(
        Results.notFound(
            notFoundPage.renderLoggedOut(requestHeader, messagesApi.preferred(requestHeader))));
  }*/

  protected CompletionStage<Result> onNotFound(Http.RequestHeader request, String message) {
    return CompletableFuture.completedFuture(Results.notFound("Page is not found"));
  }
}
