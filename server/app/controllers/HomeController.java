package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.GuestClient;
import auth.ProfileUtils;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import featureflags.FeatureFlag;
import featureflags.FeatureFlags;
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
  private final LanguageUtils languageUtils;
  private final FeatureFlags featureFlags;

  @Inject
  public HomeController(
      Config configuration,
      LoginForm form,
      ProfileUtils profileUtils,
      MessagesApi messagesApi,
      HttpExecutionContext httpExecutionContext,
      LanguageUtils languageUtils,
      FeatureFlags featureFlags) {
    checkNotNull(configuration);
    this.loginForm = checkNotNull(form);
    this.profileUtils = checkNotNull(profileUtils);
    this.messagesApi = checkNotNull(messagesApi);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.languageUtils = checkNotNull(languageUtils);
    this.faviconURL =
        Optional.ofNullable(Strings.emptyToNull(configuration.getString("whitelabel.favicon_url")));
    this.featureFlags = checkNotNull(featureFlags);
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    boolean bypassLogin =
        featureFlags.getFlagEnabled(request, FeatureFlag.BYPASS_LOGIN_LANGUAGE_SCREENS);

    // If the user isn't already logged in within their browser session, consider them a guest.
    if (maybeProfile.isEmpty()) {
      return bypassLogin
          ? CompletableFuture.completedFuture(
              redirect(routes.CallbackController.callback(GuestClient.CLIENT_NAME).url()))
          : CompletableFuture.completedFuture(
              redirect(controllers.routes.HomeController.loginForm(Optional.empty())));
    }

    // Otherwise, get the profile and go to the appropriate landing page.
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
                // Attempt to set default language for the applicant.
                applicant = languageUtils.maybeSetDefaultLocale(applicant);
                ApplicantData data = applicant.getApplicantData();
                // If the applicant has not yet set their preferred language, redirect to
                // the information controller to ask for preferred language.
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

  // TODO(#4705): remove this method
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
