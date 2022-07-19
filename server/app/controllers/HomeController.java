package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.applicant.ApplicantData;
import views.LoginForm;

/** Controller for handling methods for the landing pages. */
public class HomeController extends Controller {

  private final LoginForm loginForm;
  private final ProfileUtils profileUtils;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext httpExecutionContext;
  private final Optional<String> faviconURL;

  @Inject
  public HomeController(
      Config configuration,
      LoginForm form,
      ProfileUtils profileUtils,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext) {
    checkNotNull(configuration);
    this.loginForm = checkNotNull(form);
    this.profileUtils = checkNotNull(profileUtils);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.faviconURL =
        Optional.ofNullable(Strings.emptyToNull(configuration.getString("whitelabel.favicon_url")));
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.routes.HomeController.loginForm(Optional.empty())));
    }

    CiviFormProfile profile = maybeProfile.get();

    if (profile.isCiviFormAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramController.index()));
    } else if (profile.isProgramAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.ProgramAdminController.index()));
    } else {
      return profile
          .getApplicant()
          .thenApplyAsync(
              applicant -> {
                // If the applicant has not yet set their preferred language, redirect to
                // the information controller to ask for preferred language.
                ApplicantData data = applicant.getApplicantData();
                if (data.hasPreferredLocale()) {
                  return redirect(
                          controllers.applicant.routes.ApplicantProgramsController.index(
                              applicant.id))
                      .withLang(data.preferredLocale(), messagesApi);
                } else {
                  return redirect(
                      controllers.applicant.routes.ApplicantInformationController.edit(
                          applicant.id));
                }
              },
              httpExecutionContext.current());
    }
  }

  public Result loginForm(Http.Request request, Optional<String> message)
      throws TechnicalException {
    return ok(loginForm.render(request, messagesApi.preferred(request), message));
  }

  public Result playIndex() {
    return ok("public index");
  }

  // Redirect any browsers who, by default, request favicon from root, to the
  // specified favicon link.
  // https://stackoverflow.com/questions/56222166/prevent-browser-from-trying-to-load-favicon-from-root-directory)
  public Result favicon() {
    if (faviconURL.isPresent()) {
      return found(faviconURL.get()); // http 302
    }
    return notFound();
  }

  @Secure
  public Result securePlayIndex() {
    return ok("You are logged in.");
  }
}
