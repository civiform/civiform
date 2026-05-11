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
import models.AccountModel;
import models.StoredFileModel;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.StoredFileRepository;
import repository.VersionRepository;
import services.applicant.question.FileUploadQuestion;
import services.cloud.ApplicantFileNameFormatter;
import services.cloud.ApplicantStorageClient;
import services.settings.SettingsManifest;

/** Controller for handling methods for admins and applicants accessing uploaded files. */
public class FileController extends CiviFormController {
  private static final Logger logger = LoggerFactory.getLogger(FileController.class);

  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApplicantStorageClient applicantStorageClient;
  private final StoredFileRepository storedFileRepository;
  private final SettingsManifest settingsManifest;

  @Inject
  public FileController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      StoredFileRepository storedFileRepository,
      ApplicantStorageClient applicantStorageClient,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.applicantStorageClient = checkNotNull(applicantStorageClient);
    this.storedFileRepository = checkNotNull(storedFileRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure
  public CompletionStage<Result> show(Request request, long applicantId, String fileKey) {
    return checkApplicantAuthorization(request, applicantId)
        .thenApplyAsync(
            v -> {
              String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);
              // Ensure the file being accessed belongs to the applicant.
              // Check the file name key which embeds the applicant ID that
              // uploaded it.
              boolean hasFileNameAcl =
                  ApplicantFileNameFormatter.isApplicantOwnedFileKey(decodedFileKey, applicantId);

              boolean fileUploadImprovementsEnabled =
                  settingsManifest.getFileUploadQuestionImprovementsEnabled(request);

              Optional<StoredFileModel> storedFile = Optional.empty();
              if (fileUploadImprovementsEnabled || !hasFileNameAcl) {
                storedFile =
                    storedFileRepository.lookupFile(decodedFileKey).toCompletableFuture().join();
              }

              if (!hasFileNameAcl) {
                // Check the file ACL which may include other applicants
                // given access.
                boolean hasStoredFileAcl =
                    storedFile
                        .map(StoredFileModel::getAcls)
                        .map(acls -> acls.hasApplicantReadPermission(applicantId))
                        .orElse(false);
                if (!hasStoredFileAcl) {
                  return notFound();
                }
              }

              String downloadUrl =
                  fileUploadImprovementsEnabled
                      ? applicantStorageClient.getPresignedUrlString(
                          decodedFileKey, FileUploadQuestion.getUploadedFileName(storedFile))
                      : applicantStorageClient.getPresignedUrlString(decodedFileKey);

              return redirect(downloadUrl);
            },
            classLoaderExecutionContext.current())
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
   * action was a program admin for the specified program ID. New auth logic was introduced June 14,
   * 2022 that uses ACLs, and the CSV export used this URL until November 2024, so users with data
   * exported from CiviForm before this date require the old route to exist.
   *
   * <p>We can remove the old route if/when Seattle and Bloomington say it's alright to break the
   * old links.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result adminShow(Request request, long programId, String fileKey) {
    // File key may have PII in the filename portion, do not log it.
    logger.warn(
        "DEPRECATED: Call to /admin/programs/{}/files/:fileKey occurred. This route is obsolete.",
        programId);
    return adminShowInternal(request, fileKey);
  }

  /**
   * Action for program admins to view uploaded files. Relies on the file referenced by {@code
   * fileKey} to have {@link auth.StoredFileAcls} to authorize the caller, returns unauthorized if
   * not.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result acledAdminShow(Request request, String fileKey) {
    return adminShowInternal(request, fileKey);
  }

  /**
   * Asserts the caller has permission to view the file specified by {@code fileKey} and redirects
   * them to a presigned access URL to get the file from cloud storage. It checks access permission
   * using the stored file's {@link auth.StoredFileAcls}.
   */
  private Result adminShowInternal(Request request, String fileKey) {
    String decodedFileKey = URLDecoder.decode(fileKey, StandardCharsets.UTF_8);

    Optional<StoredFileModel> maybeFile =
        storedFileRepository.lookupFile(decodedFileKey).toCompletableFuture().join();

    if (maybeFile.isEmpty()) {
      return notFound();
    }

    AccountModel adminAccount = profileUtils.currentUserProfile(request).getAccount().join();

    // An admin is eligible if they are a global admin with the program access flag turned on
    // or if they have been explicitly given read permission to the program.
    String downloadUrl =
        settingsManifest.getFileUploadQuestionImprovementsEnabled(request)
            ? applicantStorageClient.getPresignedUrlString(
                decodedFileKey, FileUploadQuestion.getUploadedFileName(maybeFile))
            : applicantStorageClient.getPresignedUrlString(decodedFileKey);

    return ((adminAccount.getGlobalAdmin()
                && settingsManifest.getAllowCiviformAdminAccessPrograms(request))
            || maybeFile.get().getAcls().hasProgramReadPermission(adminAccount))
        ? redirect(downloadUrl)
        : unauthorized();
  }
}
