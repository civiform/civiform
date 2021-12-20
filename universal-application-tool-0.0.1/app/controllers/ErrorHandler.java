/*package controllers;

import com.typesafe.config.Config;

import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.*;
import play.mvc.*;
import play.mvc.Results;

import javax.inject.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(config, environment, sourceMapper, routes);
  }

  protected CompletionStage<Result> onNotFound(Http.RequestHeader request, String message) {
    return Results.redirect(routes.ErrorController.onNotFound().url());
  }
}*/

package controllers;

import static com.google.common.base.Preconditions.checkNotNull;
import com.typesafe.config.Config;
import com.google.inject.Provider;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;

import play.http.DefaultHttpErrorHandler;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.ProfileView;
import views.errors.NotFound;
import play.mvc.Results;

public class ErrorHandler extends DefaultHttpErrorHandler {
  private final NotFound notFoundPage;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ProfileUtils profileUtils;
  private final ProfileView profileView;

  @Inject
  public ErrorHandler(
      Config config,
      Environment environment,
      OptionalSourceMapper sourceMapper,
      Provider<Router> routes,
      ProfileUtils profileUtils,
      ProfileView profileView,
      NotFound notFoundPage,
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi) {
    super(config, environment, sourceMapper, routes);
    this.profileUtils = checkNotNull(profileUtils);
    this.profileView = checkNotNull(profileView);
    this.notFoundPage = notFoundPage;
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi = messagesApi;
  }

  public CompletionStage<Result> onNotFound(Http.RequestHeader request, String message) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(
          Results.ok(notFoundPage.renderLoggedOut(request, messagesApi.preferred(request))));
    }

    return maybeProfile
        .get()
        .getApplicant()
        .thenApplyAsync(
            applicant ->
                Results.ok( notFoundPage.renderLoggedIn(
                        request,
                        messagesApi.preferred(request),
                        applicant.getApplicantData().getApplicantName())),
            httpExecutionContext.current());
  }
}
