package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.admin.programs.ProgramEditStatus.CREATION_EDIT;

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
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
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
   * @param editStatus should match a name in the {@link views.admin.programs.ProgramEditStatus}
   *     enum.
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

  /**
   * HTMX endpoint for uploading a program summary image (stub: multipart accepted; persistence and
   * streaming parser to be added later).
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxUploadProgramImage(Http.Request request, long programId, String editStatus) {
    if (!settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      return notFound();
    }
    requestChecker.throwIfProgramNotDraft(programId);
    var body = request.body().asMultipartFormData();
    if (body != null) {
      body.getFile("file");
    }
    return ok("ok").as(Http.MimeTypes.TEXT);
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
      if (settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
        if (newDescription.isBlank()) {
          toastMessage = "Image description removed";
        } else {
          toastMessage =
              messagesApi.preferred(request).at("toast.adminProgramImage.imageSavedWithDescription")
                  + " "
                  + newDescription;
        }
      } else if (newDescription.isBlank()) {
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

    if (settingsManifest.getFileUploadQuestionImprovementsEnabled(request)) {
      String redirectUrl =
          switch (editStatus) {
            case "EDIT" -> routes.AdminProgramBlocksController.index(programId).url();
            case "CREATION", "CREATION_EDIT" ->
                routes.AdminProgramController.edit(programId, CREATION_EDIT.name()).url();
            default -> indexUrl;
          };
      return redirect(redirectUrl).flashing(toastType, toastMessage);
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
}
