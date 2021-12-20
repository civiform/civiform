package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import views.ProfileView;
import views.errors.NotFound;

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
                Results.ok(
                    notFoundPage.renderLoggedIn(
                        request,
                        messagesApi.preferred(request),
                        applicant.getApplicantData().getApplicantName())),
            httpExecutionContext.current());
  }
}
