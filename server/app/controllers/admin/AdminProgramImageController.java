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
import services.program.ProgramDefinition;
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
  public Result updateDescription(Http.Request request, long programId)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = programService.getProgramDefinition(programId);
    Form<ProgramImageDescriptionForm> form =
        formFactory
            .form(ProgramImageDescriptionForm.class)
            .bindFromRequest(
                request, ProgramImageDescriptionForm.FIELD_NAMES.toArray(new String[0]));
    String newDescription = form.get().getSummaryImageDescription();

    programService.setSummaryImageDescription(
        program.id(), LocalizedStrings.DEFAULT_LOCALE, newDescription);

    String toastMessage;
    if (newDescription.isEmpty()) {
      toastMessage = "Image description removed";
    } else {
      toastMessage = "Image description set to " + newDescription;
    }

    final String indexUrl = routes.AdminProgramImageController.index(programId).url();
    return redirect(indexUrl).flashing("success", toastMessage);
  }
}
