package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import forms.ProgramForm;
import java.util.Optional;
import javax.inject.Inject;
import models.ProgramModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.CantPublishProgramWithSharedQuestionsException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.QuestionService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramMetaDataEditView;
import views.admin.programs.ProgramNewOneView;
import views.components.ToastMessage;

/** Controller for handling methods for admins managing program definitions. */
public final class AdminProgramController extends CiviFormController {

  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramIndexView listView;
  private final ProgramNewOneView newOneView;
  private final ProgramMetaDataEditView editView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminProgramController(
      ProgramService programService,
      QuestionService questionService,
      ProgramIndexView listView,
      ProgramNewOneView newOneView,
      ProgramMetaDataEditView editView,
      VersionRepository versionRepository,
      ProfileUtils profileUtils,
      FormFactory formFactory,
      RequestChecker requestChecker,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.listView = checkNotNull(listView);
    this.newOneView = checkNotNull(newOneView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.settingsManifest = settingsManifest;
  }

  /**
   * Returns an HTML page displaying all programs of the current live version and all programs of
   * the current draft version if any.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Request request) {
    Optional<CiviFormProfile> profileMaybe = profileUtils.currentUserProfile(request);
    return ok(
        listView.render(
            programService.getActiveAndDraftProgramsWithoutQuestionLoad(),
            questionService.getReadOnlyQuestionServiceSync(),
            request,
            profileMaybe));
  }

  /** Returns an HTML page containing a form to create a new program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  /** POST endpoint for creating a new program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Request request) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm programData = programForm.bindFromRequest(request).get();

    // a null element gets added as we always have a hidden
    // option as part of the checkbox display
    while (programData.getTiGroups().remove(null)) {}
    // Display any errors with the form input to the user.
    ImmutableSet<CiviFormError> errors =
        programService.validateProgramDataForCreate(
            programData.getAdminName(),
            programData.getLocalizedDisplayName(),
            programData.getLocalizedDisplayDescription(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            ImmutableList.copyOf(programData.getTiGroups()));
    if (!errors.isEmpty()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(errors));
      return ok(newOneView.render(request, programData, message));
    }

    // If the user needs to confirm that they want to change the common intake form from a different
    // program to this one, show the confirmation dialog.
    if (settingsManifest.getIntakeFormEnabled(request)
        && programData.getIsCommonIntakeForm()
        && !programData.getConfirmedChangeCommonIntakeForm()) {
      Optional<ProgramDefinition> maybeCommonIntakeForm = programService.getCommonIntakeForm();
      if (maybeCommonIntakeForm.isPresent()) {
        return ok(
            newOneView.renderChangeCommonIntakeConfirmation(
                request, programData, maybeCommonIntakeForm.get().localizedName().getDefault()));
      }
    }

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        programService.createProgramDefinition(
            programData.getAdminName(),
            programData.getAdminDescription(),
            programData.getLocalizedDisplayName(),
            programData.getLocalizedDisplayDescription(),
            programData.getLocalizedConfirmationMessage(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            programData.getEligibilityIsGating(),
            programData.getIsCommonIntakeForm()
                ? ProgramType.COMMON_INTAKE_FORM
                : ProgramType.DEFAULT,
            settingsManifest.getIntakeFormEnabled(request),
            ImmutableList.copyOf(programData.getTiGroups()),
            programData.getCategories());
    // There shouldn't be any errors since we already validated the program, but check for errors
    // again just in case.
    if (result.isError()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
      return ok(newOneView.render(request, programData, message));
    }

    return getSaveProgramDetailsRedirect(result.getResult().id(), ProgramEditStatus.CREATION);
  }

  /**
   * Returns an HTML page containing a form to edit a draft program.
   *
   * @param editStatus should match a name in the {@link ProgramEditStatus} enum.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Request request, long id, String editStatus) throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(id);
    requestChecker.throwIfProgramNotDraft(id);
    return ok(editView.render(request, program, ProgramEditStatus.getStatusFromString(editStatus)));
  }

  /** POST endpoint for publishing all programs in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result publish() {
    try {
      versionRepository.publishNewSynchronizedVersion();
      return redirect(routes.AdminProgramController.index());
    } catch (RuntimeException e) {
      return badRequest(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result publishProgram(Request request, long programId) throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      versionRepository.publishNewSynchronizedVersion(program.adminName());
      return redirect(routes.AdminProgramController.index());
    } catch (CantPublishProgramWithSharedQuestionsException e) {
      return redirect(routes.AdminProgramController.index())
          .flashing("error", e.userFacingMessage());
    }
  }

  /** POST endpoint for creating a new draft version of the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newVersionFrom(Request request, long programId) {
    try {
      // If there's already a draft then use that, likely the client is out of date and unaware a
      // draft exists.
      // TODO(#2246): Implement FE staleness detection system to handle this more robustly.
      Optional<ProgramModel> existingDraft =
          versionRepository.getProgramByNameForVersion(
              programService.getFullProgramDefinition(programId).adminName(),
              versionRepository.getDraftVersionOrCreate());
      final Long idToEdit;
      if (existingDraft.isPresent()) {
        idToEdit = existingDraft.get().id;
      } else {
        // Make a new draft from the provided id.
        idToEdit = programService.newDraftOf(programId).id();
      }
      return redirect(controllers.admin.routes.AdminProgramBlocksController.index(idToEdit).url());
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (RuntimeException e) {
      return badRequest(e.toString());
    }
  }

  /**
   * POST endpoint for updating the program in the draft version.
   *
   * @param editStatus should match a name in the {@link ProgramEditStatus} enum.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Request request, long programId, String editStatus)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm programData = programForm.bindFromRequest(request).get();

    // a null element gets added as we always have a hidden
    // option as part of the checkbox display
    while (programData.getTiGroups().remove(null)) {}

    ProgramEditStatus programEditStatus = ProgramEditStatus.getStatusFromString(editStatus);

    // Display any errors with the form input to the user.
    ImmutableSet<CiviFormError> validationErrors =
        programService.validateProgramDataForUpdate(
            programData.getLocalizedDisplayName(),
            programData.getLocalizedDisplayDescription(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            ImmutableList.copyOf(programData.getTiGroups()));
    if (!validationErrors.isEmpty()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(validationErrors));
      return ok(
          editView.render(request, programDefinition, programEditStatus, programData, message));
    }

    // If the user needs to confirm that they want to change the common intake form from a different
    // program to this one, show the confirmation dialog.
    if (settingsManifest.getIntakeFormEnabled(request)
        && programData.getIsCommonIntakeForm()
        && !programData.getConfirmedChangeCommonIntakeForm()) {
      Optional<ProgramDefinition> maybeCommonIntakeForm = programService.getCommonIntakeForm();
      if (maybeCommonIntakeForm.isPresent()
          && !maybeCommonIntakeForm.get().adminName().equals(programDefinition.adminName())) {
        return ok(
            editView.renderChangeCommonIntakeConfirmation(
                request,
                programDefinition,
                programEditStatus,
                programData,
                maybeCommonIntakeForm.get().localizedName().getDefault()));
      }
    }

    programService.updateProgramDefinition(
        programDefinition.id(),
        LocalizedStrings.DEFAULT_LOCALE,
        programData.getAdminDescription(),
        programData.getLocalizedDisplayName(),
        programData.getLocalizedDisplayDescription(),
        programData.getLocalizedConfirmationMessage(),
        programData.getExternalLink(),
        programData.getDisplayMode(),
        programData.getEligibilityIsGating(),
        programData.getIsCommonIntakeForm() ? ProgramType.COMMON_INTAKE_FORM : ProgramType.DEFAULT,
        settingsManifest.getIntakeFormEnabled(request),
        ImmutableList.copyOf(programData.getTiGroups()));
    return getSaveProgramDetailsRedirect(programId, programEditStatus);
  }

  /** Returns where admins should be taken to after saving program detail edits. */
  private Result getSaveProgramDetailsRedirect(
      long programId, ProgramEditStatus programEditStatus) {
    if (programEditStatus == ProgramEditStatus.CREATION
        || programEditStatus == ProgramEditStatus.CREATION_EDIT) {
      // While creating a new program, we want to direct admins to also add a program image.
      return redirect(
          routes.AdminProgramImageController.index(programId, programEditStatus.name()).url());
    } else {
      return redirect(routes.AdminProgramBlocksController.index(programId).url());
    }
  }
}
