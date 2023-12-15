package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;

/**
 * Base Controller providing useful helper functions that can be utilized by all CiviForm
 * controllers.
 */
public class CiviFormController extends Controller {

  protected final ProfileUtils profileUtils;
  protected final VersionRepository versionRepository;

  @Inject
  public CiviFormController(ProfileUtils profileUtils, VersionRepository versionRepository) {
    this.profileUtils = checkNotNull(profileUtils);
    this.versionRepository = checkNotNull(versionRepository);
  }

  protected String joinErrors(ImmutableSet<CiviFormError> errors) {
    StringJoiner messageJoiner = new StringJoiner(". ", "", ".");
    for (CiviFormError e : errors) {
      messageJoiner.add(e.message());
    }
    return messageJoiner.toString();
  }

  protected CompletableFuture<Void> checkApplicantAuthorization(
      Http.Request request, long applicantId) {
    return profileUtils.currentUserProfile(request).orElseThrow().checkAuthorization(applicantId);
  }

  /**
   * Checks that the profile in {@code request} is an admin for {@code programName}.
   *
   * @throws java.util.NoSuchElementException if there is no profile in request.
   */
  protected CompletableFuture<Void> checkProgramAdminAuthorization(
      Http.Request request, String programName) {
    return profileUtils
        .currentUserProfile(request)
        .orElseThrow()
        .checkProgramAuthorization(programName, request);
  }

  /** Checks that the profile is authorized to access the specified program. */
  protected CompletionStage<Void> checkProgramAuthorization(Http.Request request, Long programId) {
    return versionRepository
        .isDraftProgramAsync(programId)
        .thenAccept(
            (isDraftProgram) -> {
              if (isDraftProgram
                  && !profileUtils.currentUserProfile(request).orElseThrow().isCiviFormAdmin()) {
                throw new SecurityException();
              }
            });
  }

  /** Retrieves the applicant id from the user profile, if present. */
  protected Optional<Long> getApplicantId(Http.Request request) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);
    if (profile.map(CiviFormProfile::getProfileData).isEmpty()) {
      return Optional.empty();
    }

    CiviFormProfileData profileData =
        profile
            .orElseThrow(() -> new MissingOptionalException(CiviFormProfileData.class))
            .getProfileData();
    return Optional.ofNullable(
        profileData.getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class));
  }

  /** Returns a redirect to the home page. */
  protected static Result redirectToHome() {
    return redirect(controllers.routes.HomeController.index().url());
  }

  /** Returns a CompletionStage containing a redirect to the home page. */
  protected static CompletionStage<Result> redirectToHomeCompletionStage() {
    return CompletableFuture.completedFuture(redirectToHome());
  }
}
