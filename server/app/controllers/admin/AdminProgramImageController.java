package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.ProgramImageDescriptionForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parsers.admin.ProgramImageStreamingMultipartBodyParser;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImagePageView;
import views.admin.programs.ProgramImagePageViewModel;
import views.admin.programs.ProgramImageView;
import views.applicant.programindex.ProgramCardsSectionParamsFactory.ProgramCardParams;

/** Controller for displaying and modifying the image (and alt text) associated with a program. */
public final class AdminProgramImageController extends CiviFormController {
  private static final Logger logger = LoggerFactory.getLogger(AdminProgramImageController.class);

  private final PublicStorageClient publicStorageClient;
  private final ProgramService programService;
  private final ProgramImageView programImageView;
  private final ProgramImagePageView programImagePageView;
  private final ProgramCardPreviewController programCardPreviewController;
  private final SettingsManifest settingsManifest;
  private final RequestChecker requestChecker;
  private final FormFactory formFactory;
  private final MessagesApi messagesApi;

  @Inject
  public AdminProgramImageController(
      PublicStorageClient publicStorageClient,
      ProgramService programService,
      ProgramImageView programImageView,
      ProgramImagePageView programImagePageView,
      ProgramCardPreviewController programCardPreviewController,
      SettingsManifest settingsManifest,
      RequestChecker requestChecker,
      FormFactory formFactory,
      MessagesApi messagesApi,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.programService = checkNotNull(programService);
    this.programImageView = checkNotNull(programImageView);
    this.programImagePageView = checkNotNull(programImagePageView);
    this.programCardPreviewController = checkNotNull(programCardPreviewController);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
    this.messagesApi = checkNotNull(messagesApi);
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
      ProgramDefinition program = programService.getFullProgramDefinition(programId);
      Optional<ProgramCardParams> cardPreviewParams = Optional.empty();
      try {
        cardPreviewParams =
            Optional.of(programCardPreviewController.programCardPreviewParams(request, program));
      } catch (RuntimeException e) {
        logger.error("Error generating card preview", e);
      }

      ProgramImagePageViewModel programImagePageViewModel =
          ProgramImagePageViewModel.builder()
              .programEditStatus(ProgramEditStatus.getStatusFromString(editStatus))
              .program(program)
              .maxFileSizeMb(publicStorageClient.getFileLimitMb())
              .cardPreviewParams(cardPreviewParams)
              .build();
      return ok(programImagePageView.render(request, programImagePageViewModel))
          .as(Http.MimeTypes.HTML);
    }
    return ok(
        programImageView.render(
            request, programService.getFullProgramDefinition(programId), editStatus));
  }

  /** Uploads a program summary image and saves its alt text. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(ProgramImageStreamingMultipartBodyParser.class)
  public Result uploadProgramImage(Http.Request request, long programId, String editStatus) {
    requestChecker.throwIfProgramNotDraft(programId);
    if (!settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      return notFound();
    }

    Http.MultipartFormData<String> body = request.body().asMultipartFormData();
    if (body == null) {
      return badRequest();
    }

    String[] descriptionValues =
        body.asFormUrlEncoded().get(ProgramImageDescriptionForm.SUMMARY_IMAGE_DESCRIPTION);
    String newDescription =
        descriptionValues != null && descriptionValues.length > 0 ? descriptionValues[0] : "";

    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    Messages messages = messagesApi.preferred(request);

    // Play's default multipart parser stores a TemporaryFile in FilePart#getRef(). Our
    // ProgramImageStreamingMultipartBodyParser streams the upload to cloud storage and stores the
    // generated file key in ref instead, which is why this is FilePart<String>.
    Http.MultipartFormData.FilePart<String> filePart = body.getFile("file");
    if (filePart != null && newDescription.isBlank()) {
      return redirect(indexUrl)
          .flashing(
              FlashKey.ERROR, messages.at("validation.adminProgramImage.altTextRequired"));
    }

    if (filePart != null) {
      String fileKey = filePart.getRef();
      if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(fileKey)) {
        throw new IllegalArgumentException(
            "Key incorrectly formatted for public program image file");
      }
      try {
        programService.setSummaryImageFileKey(programId, fileKey);
      } catch (ProgramNotFoundException e) {
        return notFound(e.toString());
      }
    }

    try {
      programService.setSummaryImageDescription(
          programId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (ImageDescriptionNotRemovableException e) {
      return redirect(indexUrl)
          .flashing(
              FlashKey.ERROR, messages.at("toast.adminProgramImage.descriptionNotRemovable"));
    }

    String successMessage =
        filePart != null
            ? messages.at("toast.adminProgramImage.imageAndDescriptionSaved", newDescription)
            : messages.at("toast.adminProgramImage.descriptionSet", newDescription);
    return redirect(indexUrl).flashing(FlashKey.SUCCESS, successMessage);
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

    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    Messages messages = messagesApi.preferred(request);

    String toastType;
    String toastMessage;
    try {
      programService.setSummaryImageDescription(
          programId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
      toastType = FlashKey.SUCCESS;
      if (newDescription.isBlank()) {
        toastMessage = messages.at("toast.adminProgramImage.descriptionRemoved");
      } else {
        toastMessage = messages.at("toast.adminProgramImage.descriptionSet", newDescription);
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (ImageDescriptionNotRemovableException e) {
      toastType = FlashKey.ERROR;
      toastMessage = messages.at("toast.adminProgramImage.descriptionNotRemovable");
    }

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
    return redirect(indexUrl)
        .flashing(
            FlashKey.SUCCESS,
            messagesApi.preferred(request).at("toast.adminProgramImage.imageSet"));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result deleteFileKey(Http.Request request, long programId, String editStatus)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    programService.deleteSummaryImageFileKey(programId);
    final String indexUrl = routes.AdminProgramImageController.index(programId, editStatus).url();
    return redirect(indexUrl)
        .flashing(
            FlashKey.SUCCESS,
            messagesApi.preferred(request).at("toast.adminProgramImage.imageRemoved"));
  }
}
