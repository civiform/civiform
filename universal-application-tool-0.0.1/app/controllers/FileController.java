package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.cloud.aws.SimpleStorage;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** Controller for handling methods for admins and applicants accessing uploaded files. */
public class FileController extends CiviFormController {
  private final HttpExecutionContext httpExecutionContext;
  private final ProgramService programService;
  private final SimpleStorage amazonS3Client;
  private final ProfileUtils profileUtils;

  @Inject
  public FileController(
      HttpExecutionContext httpExecutionContext,
      ProgramService programService,
      SimpleStorage amazonS3Client,
      ProfileUtils profileUtils) {
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.programService = checkNotNull(programService);
    this.amazonS3Client = checkNotNull(amazonS3Client);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> show(Request request, long applicantId, String fileKey) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenApplyAsync(
            v -> {
              // Ensure the file being accessed indeed belongs to the applicant.
              // The key is generated when the applicant first uploaded the file, see below link.
              // https://github.com/seattle-uat/civiform/blob/4d1e90fddd3d6da2c4a249f4f78442d08f9088d3/universal-application-tool-0.0.1/app/views/applicant/ApplicantProgramBlockEditView.java#L128
              if (!fileKey.contains(String.format("applicant-%d", applicantId))) {
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
      // Ensure the file being accessed indeed belongs to the program.
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
