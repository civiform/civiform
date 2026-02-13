package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.ProgramImageDescriptionForm;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import mapping.admin.programs.ProgramImagePageMapper;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.TranslationLocales;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.cloud.StorageUploadRequest;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramImagePageView;
import views.admin.programs.ProgramImagePageViewModel;
import views.admin.programs.ProgramImageView;
import views.fileupload.FileUploadViewStrategy;

/** Controller for displaying and modifying the image (and alt text) associated with a program. */
public final class AdminProgramImageController extends CiviFormController {
  private static final Logger logger = LoggerFactory.getLogger(AdminProgramImageController.class);

  private final PublicStorageClient publicStorageClient;
  private final ProgramService programService;
  private final ProgramImageView programImageView;
  private final ProgramImagePageView programImagePageView;
  private final RequestChecker requestChecker;
  private final FormFactory formFactory;
  private final SettingsManifest settingsManifest;
  private final FileUploadViewStrategy fileUploadViewStrategy;
  private final ProgramCardPreviewController programCardPreviewController;
  private final TranslationLocales translationLocales;
  private final String baseUrl;

  @Inject
  public AdminProgramImageController(
      PublicStorageClient publicStorageClient,
      ProgramService programService,
      ProgramImageView programImageView,
      ProgramImagePageView programImagePageView,
      RequestChecker requestChecker,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      FileUploadViewStrategy fileUploadViewStrategy,
      ProgramCardPreviewController programCardPreviewController,
      TranslationLocales translationLocales,
      Config config) {
    super(profileUtils, versionRepository);
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.programService = checkNotNull(programService);
    this.programImageView = checkNotNull(programImageView);
    this.programImagePageView = checkNotNull(programImagePageView);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.fileUploadViewStrategy = checkNotNull(fileUploadViewStrategy);
    this.programCardPreviewController = checkNotNull(programCardPreviewController);
    this.translationLocales = checkNotNull(translationLocales);
    this.baseUrl = checkNotNull(config).getString("base_url");
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

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);

      // Prepare file upload data
      String key = PublicFileNameFormatter.formatPublicProgramImageFileKey(programDefinition.id());
      String onSuccessRedirectUrl =
          baseUrl
              + routes.AdminProgramImageController.updateFileKey(programDefinition.id(), editStatus)
                  .url();
      StorageUploadRequest storageUploadRequest =
          publicStorageClient.getSignedUploadRequest(key, onSuccessRedirectUrl);
      String imageUploadFormAction = fileUploadViewStrategy.formAction(storageUploadRequest);
      if (imageUploadFormAction == null) {
        imageUploadFormAction = "";
      }

      // Card preview
      String cardPreviewHtml;
      try {
        cardPreviewHtml = programCardPreviewController.cardPreview(request, programDefinition.id());
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Error generating card preview: " + e.getLocalizedMessage());
        cardPreviewHtml = "<p>Error generating card preview</p>";
      }

      ProgramImagePageMapper mapper = new ProgramImagePageMapper();
      ProgramImagePageViewModel viewModel =
          mapper.map(
              programDefinition,
              editStatus,
              !translationLocales.translatableLocales().isEmpty(),
              imageUploadFormAction,
              fileUploadViewStrategy.getUploadFormClass(),
              fileUploadViewStrategy.additionalFileUploadFormInputFields(
                  Optional.of(storageUploadRequest)),
              publicStorageClient.getFileLimitMb(),
              cardPreviewHtml,
              request.flash().get(FlashKey.SUCCESS),
              request.flash().get(FlashKey.ERROR));
      return ok(programImagePageView.render(request, viewModel)).as(Http.MimeTypes.HTML);
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
}
