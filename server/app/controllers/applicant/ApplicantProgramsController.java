package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.applicant.ApplicantService.ApplicantProgramData;
import services.applicant.Block;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import views.applicant.ApplicantProgramInfoView;
import views.applicant.ProgramIndexView;
import views.components.ToastMessage;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public final class ApplicantProgramsController extends CiviFormController {

  private final HttpExecutionContext classLoaderExecutionContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ProgramIndexView programIndexView;
  private final ApplicantProgramInfoView programInfoView;
  private final ProgramSlugHandler programSlugHandler;
  private final ApplicantRoutes applicantRoutes;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext classLoaderExecutionContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantProgramInfoView programInfoView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramSlugHandler programSlugHandler,
      ApplicantRoutes applicantRoutes) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.programIndexView = checkNotNull(programIndexView);
    this.programInfoView = checkNotNull(programInfoView);
    this.programSlugHandler = checkNotNull(programSlugHandler);
    this.applicantRoutes = checkNotNull(applicantRoutes);
  }

  @Secure
  public CompletionStage<Result> indexWithApplicantId(Request request, long applicantId) {
    Optional<CiviFormProfile> requesterProfile = profileUtils.currentUserProfile(request);

    // If the user doesn't have a profile, send them home.
    if (requesterProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<ToastMessage> banner = request.flash().get("banner").map(ToastMessage::alert);
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        applicantService.getPersonalInfo(applicantId, request);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantProgramsForApplicant(applicantId, requesterProfile.get()),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            applicationPrograms ->
                ok(programIndexView.render(
                        messagesApi.preferred(request),
                        request,
                        applicantId,
                        applicantStage.toCompletableFuture().join(),
                        applicationPrograms,
                        banner,
                        requesterProfile.orElseThrow(
                            () -> new MissingOptionalException(CiviFormProfile.class))))
                    // If the user has been to the index page, any existing redirects should be
                    // cleared to avoid an experience where they're unexpectedly redirected after
                    // logging in.
                    .removingFromSession(request, REDIRECT_TO_SESSION_KEY),
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
        request, applicantId.orElseThrow(() -> new MissingOptionalException(Long.class)));
  }

  @Secure
  public CompletionStage<Result> showWithApplicantId(
      Request request, long applicantId, long programId) {
    Optional<CiviFormProfile> requesterProfile = profileUtils.currentUserProfile(request);

    // If the user doesn't have a profile, send them home.
    if (requesterProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId, request);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantProgramsForApplicant(applicantId, requesterProfile.get()),
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            relevantPrograms -> {
              Optional<ProgramDefinition> programDefinition =
                  relevantPrograms.allPrograms().stream()
                      .map(ApplicantProgramData::program)
                      .filter(program -> program.id() == programId)
                      .findFirst();
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
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
    Optional<CiviFormProfile> requesterProfile = profileUtils.currentUserProfile(request);

    // If the user doesn't have a profile, send them home.
    if (requesterProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    // Determine first incomplete block, then redirect to other edit.
    return checkApplicantAuthorization(request, applicantId)
        .thenComposeAsync(v -> checkProgramAuthorization(request, programId))
        .thenComposeAsync(
            v -> applicantService.getReadOnlyApplicantProgramService(applicantId, programId))
        .thenApplyAsync(
            roApplicantService -> {
              Optional<Block> blockMaybe = roApplicantService.getFirstIncompleteOrStaticBlock();
              CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
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
                CiviFormProfile profile = profileUtils.currentUserProfileOrThrow(request);
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
}
