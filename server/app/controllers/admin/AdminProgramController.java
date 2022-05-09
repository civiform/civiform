package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.ProgramForm;
import java.util.Optional;
import javax.inject.Inject;
import models.Program;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramEditView;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramNewOneView;

/** Controller for handling methods for admins managing program definitions. */
public class AdminProgramController extends CiviFormController {

  private final ProgramService service;
  private final ProgramIndexView listView;
  private final ProgramNewOneView newOneView;
  private final ProgramEditView editView;
  private final FormFactory formFactory;
  private final VersionRepository versionRepository;
  private final ProfileUtils profileUtils;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramController(
      ProgramService service,
      ProgramIndexView listView,
      ProgramNewOneView newOneView,
      ProgramEditView editView,
      VersionRepository versionRepository,
      ProfileUtils profileUtils,
      FormFactory formFactory,
      RequestChecker requestChecker) {
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.newOneView = checkNotNull(newOneView);
    this.editView = checkNotNull(editView);
    this.versionRepository = checkNotNull(versionRepository);
    this.profileUtils = checkNotNull(profileUtils);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
  }

  /**
   * Return a HTML page displaying all programs of the current live version and all programs of the
   * current draft version if any.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Request request) {
    Optional<CiviFormProfile> profileMaybe = profileUtils.currentUserProfile(request);
    return ok(listView.render(this.service.getActiveAndDraftPrograms(), request, profileMaybe));
  }

  /** Return a HTML page containing a form to create a new program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  /** POST endpoint for creating a new program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Request request) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        service.createProgramDefinition(
            program.getAdminName(),
            program.getAdminDescription(),
            program.getLocalizedDisplayName(),
            program.getLocalizedDisplayDescription(),
            program.getExternalLink(),
            program.getDisplayMode());
    if (result.isError()) {
      String errorMessage = joinErrors(result.getErrors());
      return ok(newOneView.render(request, program, errorMessage));
    }
    return redirect(routes.AdminProgramController.index().url());
  }

  /** Return a HTML page containing a form to edit a draft program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Request request, long id) {
    try {
      ProgramDefinition program = service.getProgramDefinition(id);
      return ok(editView.render(request, program));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }

  /** POST endpoint for publishing all programs in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result publish() {
    try {
      versionRepository.publishNewSynchronizedVersion();
      return redirect(routes.AdminProgramController.index());
    } catch (Exception e) {
      return badRequest(e.toString());
    }
  }

  /** POST endpoint for creating a new draft version of the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newVersionFrom(Request request, long id) {
    try {
      // If there's already a draft then use that, likely the client is out of date and unaware a
      // draft exists.
      // TODO(#2246): Implement FE staleness detection system to handle this more robustly.
      Optional<Program> existingDraft =
          versionRepository
              .getDraftVersion()
              .getProgramByName(service.getProgramDefinition(id).adminName());
      final Long idToEdit;
      if (existingDraft.isPresent()) {
        idToEdit = existingDraft.get().id;
      } else {
        // Make a new draft from the provided id.
        idToEdit = service.newDraftOf(id).id();
      }

      return redirect(routes.AdminProgramController.edit(idToEdit));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (Exception e) {
      return badRequest(e.toString());
    }
  }

  /** POST endpoint for updating the program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Request request, long programId) {
    requestChecker.throwIfNotDraft(programId);

    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          service.updateProgramDefinition(
              programId,
              LocalizedStrings.DEFAULT_LOCALE,
              program.getAdminDescription(),
              program.getLocalizedDisplayName(),
              program.getLocalizedDisplayDescription(),
              program.getExternalLink(),
              program.getDisplayMode());
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return ok(editView.render(request, programId, program, errorMessage));
      }
      return redirect(routes.AdminProgramController.index().url());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    }
  }
}
