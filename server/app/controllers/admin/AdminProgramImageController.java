package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.ProgramImageDescriptionForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import parsers.StreamingMultipartUploadResult;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImagePageView;
import views.admin.programs.ProgramImagePageViewModel;
import views.admin.programs.ProgramImageView;
import parsers.admin.ProgramImageStreamingMultipartBodyParser;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Controller for displaying and modifying the image (and alt text) associated with a program. */
public final class AdminProgramImageController extends CiviFormController {
  private final PublicStorageClient publicStorageClient;
  private final ProgramService programService;
  private final ProgramImageView programImageView;
  private final ProgramImagePageView programImagePageView;
  private final SettingsManifest settingsManifest;
  private final RequestChecker requestChecker;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramImageController(
    PublicStorageClient publicStorageClient,
    ProgramService programService,
    ProgramImageView programImageView,
    ProgramImagePageView programImagePageView,
    SettingsManifest settingsManifest,
    RequestChecker requestChecker,
    FormFactory formFactory,
    ProfileUtils profileUtils,
    VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.programService = checkNotNull(programService);
    this.programImageView = checkNotNull(programImageView);
    this.programImagePageView = checkNotNull(programImagePageView);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
  }

  /**
   * Shows the main image upload page.
   *
   * @param editStatus should match a name in the {@link ProgramEditStatus} enum.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId, String editStatus)
    throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    if (settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      return ok(programImagePageView.render(request, new ProgramImagePageViewModel()))
        .as(Http.MimeTypes.HTML);
    }
    return ok(
      programImageView.render(
        request, programService.getFullProgramDefinition(programId), editStatus));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateDescription(Http.Request request, long programId, String editStatus) {
    requestChecker.throwIfProgramNotDraft(programId);
    Form<ProgramImageDescriptionForm> form =
      formFactory
        .form(ProgramImageDescriptionForm.class)
        .bindFromRequest(
          request, ProgramImageDescriptionForm.FIELD_NAMES.toArray(new String[0]));
    String newDescription = form.get().getSummaryImageDescription();

    String toastType;
    String toastMessage;
    try {
      programService.setSummaryImageDescription(
        programId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
      toastType = "success";
      if (newDescription.isBlank()) {
        toastMessage = "Image description removed";
      } else {
        toastMessage = "Image description set to " + newDescription;
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (ImageDescriptionNotRemovableException e) {
      toastType = "error";
      toastMessage = e.getMessage();
    }

    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    return redirect(indexUrl).flashing(toastType, toastMessage);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateFileKey(Http.Request request, long programId, String editStatus)
    throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);

    // We need to check that the bucket exists even if we don't use it so that we know the request
    // was formatted correctly.
    @SuppressWarnings("unused")
    String bucket =
      request
        .queryString("bucket")
        .orElseThrow(() -> new IllegalArgumentException("Request must contain bucket name"));
    if (!bucket.equals(publicStorageClient.getBucketName())) {
      throw new IllegalArgumentException(
        String.format(
          "The bucket name in the request [%s] doesn't match the public bucket name [%s]",
          bucket, publicStorageClient.getBucketName()));
    }

    String key =
      request
        .queryString("key")
        .orElseThrow(() -> new IllegalArgumentException("Request must contain file key name"));
    // Note: If Azure support is needed, see ApplicantProgramBlocksController#updateFile for
    // some additional Azure-specific logic that should also be added here.

    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(key)) {
      throw new IllegalArgumentException("Key incorrectly formatted for public program image file");
    }

    programService.setSummaryImageFileKey(programId, key);
    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    return redirect(indexUrl).flashing(FlashKey.SUCCESS, "Image set");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result deleteFileKey(Http.Request request, long programId, String editStatus)
    throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    programService.deleteSummaryImageFileKey(programId);
    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    return redirect(indexUrl).flashing(FlashKey.SUCCESS, "Image removed");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(ProgramImageStreamingMultipartBodyParser.class)
  public CompletionStage<Result> hxUploadProgramImage(Http.Request request, long programId)
    throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);

    Http.MultipartFormData<String> body = request.body().asMultipartFormData();
    if (body == null) {
      return (CompletionStage<Result>) badRequest();
    }

    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    if (filePart == null) {
      return CompletableFuture.completedFuture(badRequest());
    }

    String originalFileName = filePart.getFilename();
    String fileKey = filePart.getRef();
    StreamingMultipartUploadResult uploadResult = ;
    if (uploadResult.getStatus() === FAILURE) {
      // return failed htmx partial
    }

    String fileKey = uploadResult.getStoredFileName();

    try {
      programService.setSummaryImageFileKey(programId, fileKey); // Save to db
// differs from applicant file upload in that the file key is stored on the program, not the StoredFileModel record, so we don't have an originalFileName to display
      ProgramDefinition program = programService.getFullProgramDefinition(programId);
      // return successful htmx partial
    } catch (ProgramNotFoundException e) {
      // handle error
    }
  }



}
