package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import java.util.Optional;
import com.google.inject.Inject;
import play.i18n.MessagesApi;
import auth.ProfileUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import play.libs.concurrent.HttpExecutionContext;

import views.errors.NotFound;
import views.ProfileView;

public class MockErrorHandler extends Controller {
  private final NotFound notFoundPage;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final ProfileUtils profileUtils;
  private final ProfileView profileView;

  @Inject
  public MockErrorHandler(
      ProfileUtils profileUtils,
      ProfileView profileView,
      NotFound notFoundPage, 
      HttpExecutionContext httpExecutionContext,
      MessagesApi messagesApi) {
    this.profileUtils = checkNotNull(profileUtils);
    this.profileView = checkNotNull(profileView);
    this.notFoundPage=notFoundPage;
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.messagesApi=messagesApi;
  }

  public CompletionStage<Result> notFound(Http.Request request) {
    /*return ok(notFoundPage.render(request, messagesApi.preferred(request), profileUtils));*/

    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(
          ok(notFoundPage.render(
                request,
                messagesApi.preferred(request)
              )));
    }

    return maybeProfile
        .get()
        .getApplicant()
        .thenApplyAsync(
            applicant -> ok(notFoundPage.render(
                request, 
                messagesApi.preferred(request), 
                applicant.getApplicantData().getApplicantName())),
            httpExecutionContext.current());
  }
}
