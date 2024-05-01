package controllers;

import static auth.DefaultToGuestRedirector.createGuestSessionAndRedirect;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.applicant.ApplicantData;

/** Controller for handling methods for the landing pages. */
public class HomeController extends Controller {

  private final ProfileUtils profileUtils;
  private final MessagesApi messagesApi;
  private final HttpExecutionContext classLoaderExecutionContext;
  private final Optional<String> faviconURL;
  private final LanguageUtils languageUtils;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public HomeController(
      Config configuration,
      ProfileUtils profileUtils,
      MessagesApi messagesApi,
      HttpExecutionContext classLoaderExecutionContext,
      LanguageUtils languageUtils,
      ApplicantRoutes applicantRoutes) {
    checkNotNull(configuration);
    this.profileUtils = checkNotNull(profileUtils);
    this.messagesApi = checkNotNull(messagesApi);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.languageUtils = checkNotNull(languageUtils);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.faviconURL =
        Optional.ofNullable(Strings.emptyToNull(configuration.getString("favicon_url")));
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<CiviFormProfile> maybeProfile = profileUtils.currentUserProfile(request);

    // If the user isn't already logged in within their browser session, consider them a guest.
    if (maybeProfile.isEmpty()) {
      return CompletableFuture.completedFuture(createGuestSessionAndRedirect(request));
    }

    // Otherwise, get the profile and go to the appropriate landing page.
    CiviFormProfile profile = maybeProfile.get();

    if (profile.isCiviFormAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramController.index()));
    } else if (profile.isProgramAdmin()) {
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.ProgramAdminController.index()));
    } else if (profile.isTrustedIntermediary()) {
      return CompletableFuture.completedFuture(
          redirect(
              controllers.ti.routes.TrustedIntermediaryController.dashboard(
                  /* nameQuery= */ Optional.empty(),
                  /* dayQuery= */ Optional.empty(),
                  /* monthQuery= */ Optional.empty(),
                  /* yearQuery= */ Optional.empty(),
                  /* page= */ Optional.empty())));
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
                  return redirect(applicantRoutes.index(profile, applicant.id))
                      .withLang(data.preferredLocale(), messagesApi);
                } else {
                  return redirect(
                      controllers.applicant.routes.ApplicantInformationController
                          .setLangFromBrowser(applicant.id));
                }
              },
              classLoaderExecutionContext.current());
    }
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

  public Result teapot() {
    return status(
        418,
        "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣤⣤⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣘⣿⣿⣀⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⣀⣀⡀⠀⠀⠀⢀⣀⠘⠛⠛⠛⠛⠛⠛⠁⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⢠⡿⠋⠉⠛⠃⣠⣤⣈⣉⡛⠛⠛⠛⠛⠛⠛⢛⣉⣁⣤⣄⠀⠀⣾⣿⡿⠗⠀\n"
            + "⠀⢸⡇⠀⠀⠀⣰⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⣿⣿⠀⠀⠀\n"
            + "⠀⢸⣇⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠟⢉⣉⣠⣿⣿⡀⠀⠀\n"
            + "⠀⠀⠙⠷⡆⠘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠃⢰⣿⣿⣿⣿⣿⡇⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⢻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⠸⣿⣿⣿⣿⠟⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠙⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠄⠈⠉⠁⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⢄⣉⠉⠛⠛⠛⠛⠛⠋⢉⣉⡠⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⠻⠿⠿⠿⠿⠿⠿⠛⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n"
            + "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n");
  }
}
