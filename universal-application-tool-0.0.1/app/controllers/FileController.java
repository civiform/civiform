package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.StoredFile;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.ApplicantService;
import services.aws.SimpleStorage;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

public class FileController extends CiviFormController {
  private final HttpExecutionContext httpExecutionContext;
  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final SimpleStorage amazonS3Client;
  private final ProfileUtils profileUtils;

  @Inject
  public FileController(
      HttpExecutionContext httpExecutionContext,
      ApplicantService applicantService,
      ProgramService programService,
      SimpleStorage amazonS3Client,
      ProfileUtils profileUtils) {
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.amazonS3Client = checkNotNull(amazonS3Client);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> show(Request request, long applicantId, String fileKey) {
    CompletionStage<Applicant> applicantStage = applicantService.getApplicant(applicantId);
    return applicantStage
        .thenComposeAsync(
            v -> checkApplicantAuthorization(profileUtils, request, applicantId),
            httpExecutionContext.current())
        .thenApplyAsync(
            v -> {
              Applicant applicant = applicantStage.toCompletableFuture().join();
              // Check if the referenced file is owned by the applicant.
              Optional<String> maybeKey =
                  applicant.getStoredFiles().stream()
                      .map(StoredFile::getName)
                      .filter(key -> key.equals(fileKey))
                      .findAny();
              if (maybeKey.isEmpty()) {
                return notFound();
              }
              return redirect(amazonS3Client.getPresignedUrl(fileKey).toString());
            },
            httpExecutionContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof SecurityException) {
                  return unauthorized();
                }
              }
              throw new RuntimeException(ex);
            });
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result adminShow(Request request, long programId, String fileKey) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      // This ensures the file the admin is accessing belongs to an application applying to the
      // program they administer.
      if (!fileKey.contains(String.format("program-%d", programId))) {
        return notFound();
      }
      return redirect(amazonS3Client.getPresignedUrl(fileKey).toString());
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      // This is only possible when checkProgramAdminAuthorization fails.
      return unauthorized();
    }
  }
}
