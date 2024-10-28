package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicationPrograms;
import services.applicant.Block;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.settings.SettingsManifest;
import views.applicant.ApplicantDisabledProgramView;
import views.applicant.ApplicantProgramInfoView;
import views.applicant.NorthStarApplicantProgramOverviewView;
import views.applicant.NorthStarProgramIndexView;
import views.applicant.ProgramIndexView;
import views.components.ToastMessage;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public final class ApplicantProgramsController extends CiviFormController {

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ProgramIndexView programIndexView;
  private final ApplicantDisabledProgramView disabledProgramInfoView;
  private final ProgramSlugHandler programSlugHandler;
  private final ApplicantRoutes applicantRoutes;
  private final SettingsManifest settingsManifest;
  private final NorthStarProgramIndexView northStarProgramIndexView;
  private final NorthStarApplicantProgramOverviewView northStarApplicantProgramOverviewView;
  private final ProgramRepository programRepository;

  @Inject
  public ApplicantProgramsController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantDisabledProgramView disabledProgramInfoView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      NorthStarProgramIndexView northStarProgramIndexView,
      NorthStarApplicantProgramOverviewView northStarApplicantProgramOverviewView,
      ProgramRepository programRepository) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.disabledProgramInfoView = checkNotNull(disabledProgramInfoView);
    this.programIndexView = checkNotNull(programIndexView);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.northStarProgramIndexView = checkNotNull(northStarProgramIndexView);
    this.northStarApplicantProgramOverviewView =
        checkNotNull(northStarApplicantProgramOverviewView);
    this.programRepository = checkNotNull(programRepository);
  }

  @Secure
  public CompletionStage<Result> indexWithApplicantId(
      Request request,
      long applicantId,
      List<String> categories /* The selected program categories */) {
    CiviFormProfile requesterProfile = profileUtils.currentUserProfile(request);

    Optional<String> bannerMessage = request.flash().get(FlashKey.BANNER);
    Optional<ToastMessage> banner = bannerMessage.map(ToastMessage::alert);
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
              if (settingsManifest.getNorthStarApplicantUi(request)) {
                result =
                    ok(northStarProgramIndexView.render(
                            messagesApi.preferred(request),
                            request,
                            Optional.of(applicantId),
                            applicantStage.toCompletableFuture().join(),
                            applicationPrograms,
                            bannerMessage,
                            Optional.of(requesterProfile)))
                        .as(Http.MimeTypes.HTML);
              } else {
                result =
                    ok(
                        programIndexView.render(
                            messagesApi.preferred(request),
                            request,
                            Optional.of(applicantId),
                            applicantStage.toCompletableFuture().join(),
                            applicationPrograms,
                            ImmutableList.copyOf(categories),
                            banner,
                            Optional.of(requesterProfile)));
              }
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
        applicantService.relevantProgramsWithoutApplicant(request).toCompletableFuture();

    return programsFuture.thenApplyAsync(
        programs -> {
          return settingsManifest.getNorthStarApplicantUi(request)
              ? ok(northStarProgramIndexView.render(
                      messagesApi.preferred(request),
                      request,
                      Optional.empty(),
                      ApplicantPersonalInfo.ofGuestUser(),
                      programsFuture.join(),
                      request.flash().get(FlashKey.BANNER),
                      Optional.empty()))
                  .as(Http.MimeTypes.HTML)
              : ok(
                  programIndexView.renderWithoutApplicant(
                      messagesApi.preferred(request),
                      request,
                      programs,
                      ImmutableList.copyOf(categories)));
        });
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

  // This controller method disambiguates between two routes:
  //
  // - /programs/<program-id>
  // - /programs/<program-slug>
  //
  // The program id route is deprecated, so it always redirects to home.
  public CompletionStage<Result> show(Request request, String programParam) {
    if (StringUtils.isNumeric(programParam)) {
      // We no longer support (or provide) links to numeric program ID (See issue #8599), redirect
      // to home.
      return CompletableFuture.completedFuture(redirectToHome());
    } else {
      // here we want to check if north star is enabled
      // if it is, redirect to the overview page
      // if it's not, do this old behavior
      if (settingsManifest.getNorthStarApplicantUi(request)) {
        return programRepository
            .getActiveProgramFromSlug(programParam)
            .thenApply(
                program -> {
                  ProgramDefinition programDefinition = program.getProgramDefinition();
                  NorthStarApplicantProgramOverviewView.Params params =
                      NorthStarApplicantProgramOverviewView.Params.builder()
                          .setRequest(request)
                          .setApplicantPersonalInfo(ApplicantPersonalInfo.ofGuestUser())
                          .setProgramDefinition(programDefinition)
                          .setMessages(messagesApi.preferred(request))
                          .build();
                  return ok(northStarApplicantProgramOverviewView.render(params))
                      .as(Http.MimeTypes.HTML);
                });
      } else {
        return programSlugHandler.showProgram(this, request, programParam);
      }
    }
  }

  @Secure
  public CompletionStage<Result> editWithApplicantId(
      Request request, long applicantId, long programId) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    // Determine first incomplete block, then redirect to other edit.
    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId))
        .thenApplyAsync(
            roApplicantService -> {
              Optional<Block> blockMaybe = roApplicantService.getFirstIncompleteOrStaticBlock();
              return blockMaybe.flatMap(
                  block ->
                      Optional.of(
                          found(
                              applicantRoutes.blockEdit(
                                  profile,
                                  applicantId,
                                  programId,
                                  block.getId(),
                                  /* questionName= */ Optional.empty()))));
            },
            classLoaderExecutionContext.current())
        .thenComposeAsync(
            resultMaybe -> {
              if (resultMaybe.isEmpty()) {
                return supplyAsync(
                    () -> redirect(applicantRoutes.review(profile, applicantId, programId)));
              }
              return supplyAsync(resultMaybe::get);
            },
            classLoaderExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  // If the applicant id in the URL does not correspond to the current user, start
                  // from scratch. This could happen if a user bookmarks a URL.
                  return redirectToHome();
                }
                if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure
  public CompletionStage<Result> edit(Request request, long programId) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return editWithApplicantId(request, applicantId.orElseThrow(), programId);
  }

  @Secure
  public CompletionStage<Result> showInfoDisabledProgram(Request request, String programSlug) {
    Optional<Long> applicantId = getApplicantId(request);
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId.get());
    return CompletableFuture.completedFuture(
        Results.notFound(
            disabledProgramInfoView.render(
                messagesApi.preferred(request),
                request,
                applicantId.orElseThrow(),
                applicantStage.toCompletableFuture().join())));
  }
}
