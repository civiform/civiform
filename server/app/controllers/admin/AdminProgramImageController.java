package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.admin.ProgramImageDescriptionForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramImageView;

/** Controller for displaying and modifying the image (and alt text) associated with a program. */
public final class AdminProgramImageController extends CiviFormController {
  private final ProgramService programService;
  private final ProgramImageView programImageView;
  private final RequestChecker requestChecker;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramImageController(
      ProgramService programService,
      ProgramImageView programImageView,
      RequestChecker requestChecker,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.programService = checkNotNull(programService);
    this.programImageView = checkNotNull(programImageView);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId, String referer)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    return ok(
        programImageView.render(request, programService.getProgramDefinition(programId), referer));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateDescription(Http.Request request, long programId, String referer) {
    requestChecker.throwIfProgramNotDraft(programId);
    Form<ProgramImageDescriptionForm> form =
        formFactory
            .form(ProgramImageDescriptionForm.class)
            .bindFromRequest(
                request, ProgramImageDescriptionForm.FIELD_NAMES.toArray(new String[0]));
    String newDescription = form.get().getSummaryImageDescription();

    try {
      programService.setSummaryImageDescription(
          programId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }

    String toastMessage;
    if (newDescription.isBlank()) {
      toastMessage = "Image description removed";
    } else {
      toastMessage = "Image description set to " + newDescription;
    }

    final String indexUrl = routes.AdminProgramImageController.index(programId, referer).url();
    return redirect(indexUrl).flashing("success", toastMessage);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateFileKey(Http.Request request, long programId, String referer)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);

    // We need to check that the bucket exists even if we don't use it so that we know the request
    // was formatted correctly.
    @SuppressWarnings("unused")
    String bucket =
        request
            .queryString("bucket")
            .orElseThrow(() -> new IllegalArgumentException("Request must contain bucket name"));
    // TODO(#5676): Verify the bucket name is for the public bucket?

    String key =
        request
            .queryString("key")
            .orElseThrow(() -> new IllegalArgumentException("Request must contain file key name"));
    // TODO(#5676): If Azure support is needed, see ApplicantProgramBlocksController#updateFile for
    // some additional Azure-specific logic that's needed.

    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(key)) {
      throw new IllegalArgumentException("Key incorrectly formatted for public program image file");
    }

    programService.setSummaryImageFileKey(programId, key);
    final String indexUrl = routes.AdminProgramImageController.index(programId, referer).url();
    return redirect(indexUrl).flashing("success", "Image set");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result deleteFileKey(Http.Request request, long programId, String referer)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    programService.deleteSummaryImageFileKey(programId);
    final String indexUrl = routes.AdminProgramImageController.index(programId, referer).url();
    return redirect(indexUrl).flashing("success", "Image removed");
  }

  /**
   * Enum specifying how an admin got to the program image page. This is used to ensure the "Back"
   * button goes to the right place.
   */
  public enum Referer {
    /** The admin came from the program creation page. */
    CREATION,
    /** The admin came from the edit program blocks page. */
    BLOCKS,
  }
}
