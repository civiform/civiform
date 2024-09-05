package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
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
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.Block;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.settings.SettingsManifest;
import views.applicant.ApplicantDisabledProgramView;
import views.applicant.ApplicantProgramInfoView;
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
  private final ApplicantProgramInfoView programInfoView;
  private final ProgramSlugHandler programSlugHandler;
  private final ApplicantRoutes applicantRoutes;
  private final SettingsManifest settingsManifest;
  private final NorthStarProgramIndexView northStarProgramIndexView;

  @Inject
  public ApplicantProgramsController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantDisabledProgramView disabledProgramInfoView,
      ApplicantProgramInfoView programInfoView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      NorthStarProgramIndexView northStarProgramIndexView) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.disabledProgramInfoView = checkNotNull(disabledProgramInfoView);
    this.programIndexView = checkNotNull(programIndexView);
    this.programInfoView = checkNotNull(programInfoView);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.northStarProgramIndexView = checkNotNull(northStarProgramIndexView);
  }

  @Secure
  public CompletionStage<Result> indexWithApplicantId(
      Request request,
      long applicantId, /* The selected program categories */
      List<String> categories) {
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
                            applicantId,
                            applicantStage.toCompletableFuture().join(),
                            applicationPrograms,
                            bannerMessage,
                            requesterProfile))
                        .as(Http.MimeTypes.HTML);
              } else {
                result =
                    ok(
                        programIndexView.render(
                            messagesApi.preferred(request),
                            request,
                            applicantId,
                            applicantStage.toCompletableFuture().join(),
                            applicationPrograms,
                            ImmutableList.copyOf(categories),
                            banner,
                            requesterProfile));
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

  @Secure
  public CompletionStage<Result> index(Request request) {
    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    return indexWithApplicantId(
        request,
        applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)),
        ImmutableList.of());
  }

  @Secure
  public CompletionStage<Result> showWithApplicantId(
      Request request, long applicantId, long programId) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantProgramsForApplicant(applicantId, profile, request),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            relevantPrograms -> {
              Optional<ProgramDefinition> programDefinition =
                  relevantPrograms.allPrograms().stream()
                      .map(ApplicantProgramData::program)
                      .filter(program -> program.id() == programId)
                      .findFirst();
              if (programDefinition.isPresent()) {
                return ok(
                    programInfoView.render(
                        messagesApi.preferred(request),
                        programDefinition.get(),
                        request,
                        applicantId,
                        applicantStage.toCompletableFuture().join(),
                        profile));
              }
              return badRequest(String.format("Program %d not found", programId));
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

  // This controller method disambiguates between two routes:
  //
  // - /programs/<program-id>
  // - /programs/<program-slug>
  //
  // Because the second use is public, this controller is not annotated as @Secure. For the first
  // use, the delegated-to method *is* annotated as such.
  public CompletionStage<Result> show(Request request, String programParam) {
    if (StringUtils.isNumeric(programParam)) {
      // The path parameter specifies a program by (numeric) id.
      Optional<Long> applicantId = getApplicantId(request);
      if (applicantId.isEmpty()) {
        // This route should not have been computed for the user in this case, but they may have
        // gotten the URL from another source.
        return CompletableFuture.completedFuture(redirectToHome());
      }
      return showWithApplicantId(
          request,
          applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)),
          Long.parseLong(programParam));
    } else {
      return programSlugHandler.showProgram(this, request, programParam);
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
