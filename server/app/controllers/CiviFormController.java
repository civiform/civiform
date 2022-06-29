package controllers;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import play.mvc.Controller;
import play.mvc.Http;
import services.CiviFormError;
import services.DateConverter;

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

  protected CompletableFuture<Void> checkProgramAdminAuthorization(
      ProfileUtils profileUtils, Http.Request request, String programName) {
    return profileUtils
        .currentUserProfile(request)
        .orElseThrow()
        .checkProgramAuthorization(programName);
  }

  protected final Optional<Instant> parseDateFromQuery(
      DateConverter dateConverter, Optional<String> maybeQueryParam) {
    return maybeQueryParam
        .filter(s -> !s.isBlank())
        .map(
            s -> {
              try {
                return dateConverter.parseIso8601DateToStartOfDateInstant(s);
              } catch (DateTimeParseException e) {
                throw new BadRequestException("Malformed query param");
              }
            });
  }
}
