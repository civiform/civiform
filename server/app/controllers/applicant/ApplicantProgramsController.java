package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.ProfileUtils;
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

  private final HttpExecutionContext httpContext;
  private final ApplicantService applicantService;
  private final MessagesApi messagesApi;
  private final ProgramIndexView programIndexView;
  private final ApplicantProgramInfoView programInfoView;

  @Inject
  public ApplicantProgramsController(
      HttpExecutionContext httpContext,
      ApplicantService applicantService,
      MessagesApi messagesApi,
      ProgramIndexView programIndexView,
      ApplicantProgramInfoView programInfoView,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.httpContext = checkNotNull(httpContext);
    this.applicantService = checkNotNull(applicantService);
    this.messagesApi = checkNotNull(messagesApi);
    this.programIndexView = checkNotNull(programIndexView);
    this.programInfoView = checkNotNull(programInfoView);
  }

  @Secure
  public CompletionStage<Result> indexWithApplicantId(Request request, long applicantId) {
    Optional<CiviFormProfile> requesterProfile = profileUtils.currentUserProfile(request);

    // If the user doesn't have a profile, send them home.
    if (requesterProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<ToastMessage> banner = request.flash().get("banner").map(m -> ToastMessage.alert(m));
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantProgramsForApplicant(applicantId, requesterProfile.get()),
            httpContext.current())
        .thenApplyAsync(
            applicationPrograms -> {
              return ok(programIndexView.render(
                      messagesApi.preferred(request),
                      request,
                      applicantId,
                      applicantStage.toCompletableFuture().join(),
                      applicationPrograms,
                      banner,
                      requesterProfile.orElseThrow()))
                  // If the user has been to the index page, any existing redirects should be
                  // cleared to avoid an experience where they're unexpectedly redirected after
                  // logging in.
                  .removingFromSession(request, REDIRECT_TO_SESSION_KEY);
            },
            httpContext.current())
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
    // The route for this action should only be computed if the applicant ID is available in the
    // session.
    long applicantId = getApplicantId(request).orElseThrow();
    return indexWithApplicantId(request, applicantId);
  }

  @Secure
  public CompletionStage<Result> viewWithApplicantId(
      Request request, long applicantId, long programId) {
    Optional<CiviFormProfile> requesterProfile = profileUtils.currentUserProfile(request);

    // If the user doesn't have a profile, send them home.
    if (requesterProfile.isEmpty()) {
      return CompletableFuture.completedFuture(redirectToHome());
    }

    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId);

    return applicantStage
        .thenComposeAsync(v -> checkApplicantAuthorization(request, applicantId))
        .thenComposeAsync(
            v -> applicantService.relevantProgramsForApplicant(applicantId, requesterProfile.get()),
            httpContext.current())
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
                        applicantStage.toCompletableFuture().join()));
              }
              return badRequest();
            },
            httpContext.current())
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
  // This method can be invoked for two different logical routes:
  // - /programs/:id
  // - /programs/:programSlug
  //
  // However, play does not allow overloaded controller methods, so we determine which treatment to apply based on the form of the parameter.
  public CompletionStage<Result> view(Request request, String programParam) {
    if (StringUtils.isNumeric(programParam)) {
      // programParam is a numeric program id.

      // The route for this action should only be computed if the applicant ID is available in the
      // session.
      long applicantId = getApplicantId(request).orElseThrow();

      return viewWithApplicantId(request, applicantId, Long.parseLong(programParam));
    } else {
      // programParam is not a numeric id, so treat it like a program slug.
      return programBySlug(request, programParam);
    }
  }

  @Secure
  public CompletionStage<Result> edit(Request request, long applicantId, long programId) {
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
              return blockMaybe.flatMap(
                  block ->
                      Optional.of(
                          found(
                              routes.ApplicantProgramBlocksController.edit(
                                  applicantId,
                                  programId,
                                  block.getId(),
                                  /* questionName= */ Optional.empty()))));
            },
            httpContext.current())
        .thenComposeAsync(
            resultMaybe -> {
              if (resultMaybe.isEmpty()) {
                return supplyAsync(
                    () ->
                        redirect(
                            routes.ApplicantProgramReviewController.review(
                                applicantId, programId)));
              }
              return supplyAsync(resultMaybe::get);
            },
            httpContext.current())
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

  private static Result redirectToHome() {
    return redirect(controllers.routes.HomeController.index().url());
  }
}
