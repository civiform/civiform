package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import models.StoredFile;
import org.pac4j.play.java.Secure;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import services.cloud.StorageClient;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** Controller for handling methods for admins and applicants accessing uploaded files. */
public class FileController extends CiviFormController {
  private final HttpExecutionContext httpExecutionContext;
  private final ProgramService programService;
  private final StorageClient storageClient;
  private final StoredFileRepository storedFileRepository;
  private final ProfileUtils profileUtils;

  @Inject
  public FileController(
      HttpExecutionContext httpExecutionContext,
      ProgramService programService,
      StoredFileRepository storedFileRepository,
      StorageClient storageClient,
      ProfileUtils profileUtils) {
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
    this.programService = checkNotNull(programService);
    this.storageClient = checkNotNull(storageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure
  public CompletionStage<Result> show(Request request, long applicantId, String fileKey) {
    return checkApplicantAuthorization(profileUtils, request, applicantId)
        .thenApplyAsync(
            v -> {
              // Ensure the file being accessed indeed belongs to the applicant.
              // The key is generated when the applicant first uploaded the file, see below link.
              // https://github.com/seattle-uat/civiform/blob/4d1e90fddd3d6da2c4a249f4f78442d08f9088d3/server/app/views/applicant/ApplicantProgramBlockEditView.java#L128
              if (!fileKey.contains(String.format("applicant-%d", applicantId))) {
                return notFound();
              }
              String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
              return redirect(storageClient.getPresignedUrlString(decodedFileKey));
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

  /**
   * The legacy show action for admins viewing uploaded files. Needed because file URLs exported
   * before the move to using {@link auth.StoredFileAcls} relied on asserting that the program ID
   * embedded in the {@code fileKey} param matched the one in the URL and that the user calling the
   * action was a program admin for the specified program ID.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result adminShow(Request request, long programId, String fileKey) {
    return adminShowInternal(request, Optional.of(programId), fileKey);
  }

  /**
   * Action for program admins to view uploaded files. Relies on the file referenced by {@code
   * fileKey} to have {@link auth.StoredFileAcls} to authorize the caller, returns unauthorized if
   * not.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result acledAdminShow(Request request, String fileKey) {
    return adminShowInternal(request, /* maybeProgramId= */ Optional.empty(), fileKey);
  }

  /**
   * Asserts the caller has permission to view the file specified by {@code fileKey} and redirects
   * them to a presigned access URL to get the file from cloud storage. It first attempts to check
   * access permission using the stored file's {@link auth.StoredFileAcls}. If they are not present,
   * it falls back to the legacy auth logic of checking the if the caller is an admin of the program
   * identified by {@code maybeProgramId} if it is present.
   */
  private Result adminShowInternal(Request request, Optional<Long> maybeProgramId, String fileKey) {
    Optional<StoredFile> maybeFile =
        storedFileRepository.lookupFile(fileKey).toCompletableFuture().join();

    if (maybeFile.isEmpty()) {
      return notFound();
    }

    Account adminAccount =
        profileUtils.currentUserProfile(request).orElseThrow().getAccount().join();

    if (!maybeFile.get().getAcls().hasProgramReadPermission(adminAccount)) {
      // If the request includes a program ID in the URL, try the legacy logic
      // that assumes the admin is associated with the original program the
      // file was uploaded for.
      return maybeProgramId
          .map(programId -> legacyAdminShow(request, programId, fileKey))
          .orElse(unauthorized());
    }

    String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
    return redirect(storageClient.getPresignedUrlString(decodedFileKey));
  }

  /**
   * The old auth logic assumes that the admin viewing the file should be an admin for the original
   * program the file was uploaded for. This is not the case, since file upload questions can be
   * shared between programs.
   */
  // TODO(https://github.com/seattle-uat/civiform/issues/2709): add ACLs to previously uploaded
  // StoredFiles
  private Result legacyAdminShow(Request request, long programId, String fileKey) {
    try {
      ProgramDefinition program = programService.getProgramDefinition(programId);
      checkProgramAdminAuthorization(profileUtils, request, program.adminName()).join();
      // Ensure the file being accessed indeed belongs to the program.
      if (!fileKey.contains(String.format("program-%d", programId))) {
        return notFound();
      }
      String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
      return redirect(storageClient.getPresignedUrlString(decodedFileKey));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (CompletionException e) {
      // This is only possible when checkProgramAdminAuthorization fails.
      return unauthorized();
    }
  }
}
