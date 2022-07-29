package controllers;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import play.mvc.Controller;
import play.mvc.Http;
import services.CiviFormError;

/**
 * Base Controller providing useful helper functions that can be utilized by all CiviForm
 * controllers.
 */
public class CiviFormController extends Controller {

  protected String joinErrors(ImmutableSet<CiviFormError> errors) {
    StringJoiner messageJoiner = new StringJoiner(". ", "", ".");
    for (CiviFormError e : errors) {
      messageJoiner.add(e.message());
    }
    return messageJoiner.toString();
  }

  protected CompletableFuture<Void> checkApplicantAuthorization(
      ProfileUtils profileUtils, Http.Request request, long applicantId) {
    return profileUtils.currentUserProfile(request).orElseThrow().checkAuthorization(applicantId);
  }

  /**
   * Checks that the profile in {@code request} is an admin for {@code programName}.
   *
   * @throws java.util.NoSuchElementException if there is no profile in request.
   */
  protected CompletableFuture<Void> checkProgramAdminAuthorization(
      ProfileUtils profileUtils, Http.Request request, String programName) {
    return profileUtils
        .currentUserProfile(request)
        .orElseThrow()
        .checkProgramAuthorization(programName);
  }
}
