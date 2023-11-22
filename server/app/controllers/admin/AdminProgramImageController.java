package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.admin.ProgramImageDescriptionForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
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
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    return ok(programImageView.render(request, programService.getProgramDefinition(programId)));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateDescription(Http.Request request, long programId) {
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

    final String indexUrl = routes.AdminProgramImageController.index(programId).url();
    return redirect(indexUrl).flashing("success", toastMessage);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateFileKey(Http.Request request, long programId)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);

    // From ApplicantProgramBlocksController#updateFile
    Optional<String> bucket = request.queryString("bucket");
    Optional<String> key = request.queryString("key");
    System.out.println("bucket = " + bucket + "  key=" + key);
    // TODO: There's some azure stuff here

    if (bucket.isEmpty() || key.isEmpty()) {
      throw new IllegalArgumentException(); // TODO
      // return failedFuture(
      //    new IllegalArgumentException("missing file key and bucket names"));
    }

    // TODO(#5676): Verify description has been set before allowing image upload.
    programService.setSummaryImageFileKey(programId, key.get());
    final String indexUrl = routes.AdminProgramImageController.index(programId).url();
    return redirect(indexUrl).flashing("success", "Image set");
  }
}
