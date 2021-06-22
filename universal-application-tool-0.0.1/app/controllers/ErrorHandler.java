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

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

  @Inject
  public ErrorHandler(
      Configuration config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(environment, config, sourceMapper, routes);
  }

  protected CompletionStage<Result> onNotFound(Http.RequestHeader request, String message) {
    return CompletableFuture.completedFuture(
        Results.forbidden("Page not found. Check URL is correct"));
  }
}
