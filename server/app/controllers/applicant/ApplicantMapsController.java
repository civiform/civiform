package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicationPrograms;
import views.applicant.MapsView;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public final class ApplicantMapsController extends CiviFormController {

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final MapsView mapsView;

  @Inject
  public ApplicantMapsController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      MapsView mapsView) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.mapsView = checkNotNull(mapsView);
  }

  @Secure
  public CompletionStage<Result> indexWithApplicantId(
      Request request,
      long applicantId,
      List<String> categories /* The selected program categories */) {
    CiviFormProfile requesterProfile = profileUtils.currentUserProfile(request);

    Optional<String> bannerMessage = request.flash().get(FlashKey.BANNER);
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v ->
                applicantService.relevantProgramsForApplicant(
                    applicantId, requesterProfile, request),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            applicationPrograms -> {
              Result result;
              result =
                  ok(mapsView.render(
                          messagesApi.preferred(request),
                          request,
                          Optional.of(applicantId),
                          applicantStage.toCompletableFuture().join(),
                          applicationPrograms,
                          bannerMessage,
                          Optional.of(requesterProfile)))
                      .as(Http.MimeTypes.HTML);
              // If the user has been to the index page, any existing redirects should be
              // cleared to avoid an experience where they're unexpectedly redirected after
              // logging in.
              return result.removingFromSession(request, REDIRECT_TO_SESSION_KEY);
            },
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                if (ex.getCause() instanceof SecurityException) {
                  // If the applicant id in the URL does not correspond to the current user, start
                  // from scratch. This could happen if a user bookmarks a URL.
                  return redirectToHome();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  /**
   * When the user is not logged in or tied to a guest account, show them the list of publicly
   * viewable programs.
   */
  public CompletionStage<Result> indexWithoutApplicantId(Request request, List<String> categories) {
    CompletableFuture<ApplicationPrograms> programsFuture =
        applicantService.relevantProgramsWithoutApplicant().toCompletableFuture();

    return programsFuture.thenApplyAsync(
        programs ->
            ok(mapsView.render(
                    messagesApi.preferred(request),
                    request,
                    Optional.empty(),
                    ApplicantPersonalInfo.ofGuestUser(),
                    programsFuture.join(),
                    request.flash().get(FlashKey.BANNER),
                    Optional.empty()))
                .as(Http.MimeTypes.HTML));
  }

  public CompletionStage<Result> index(Request request, List<String> categories) {
    if (profileUtils.optionalCurrentUserProfile(request).isEmpty()) {
      return indexWithoutApplicantId(request, categories);
    }

    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return indexWithoutApplicantId(request, categories);
    }
    return indexWithApplicantId(request, applicantId.get(), categories);
  }

  //  public Result getProviders() {
  //    List<Provider> providerStats = new ArrayList<>();
  //    Provider provider = new Provider();
  //    provider.setState("California");
  //    provider.setAddress("US");
  //    provider.setLatitude(37.877102);
  //    provider.setLongitude(-122.289917);
  //    provider.setLatestTotalCases(1);
  //    providerStats.add(provider);
  //    System.out.println(providerStats);
  //    return ok(providerStats.toString());
  //  }
}
