package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.ProgramForm;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.programs.ProgramEditPageMapper;
import mapping.admin.programs.ProgramIndexPageMapper;
import mapping.admin.programs.ProgramNewOnePageMapper;
import models.ApplicationStep;
import models.ProgramModel;
import models.ProgramTab;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.AccountRepository;
import repository.CategoryRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ActiveAndDraftPrograms;
import services.program.CantPublishProgramWithSharedQuestionsException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramEditPageView;
import views.admin.programs.ProgramEditPageViewModel;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramIndexPageView;
import views.admin.programs.ProgramIndexPageViewModel;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramMetaDataEditView;
import views.admin.programs.ProgramNewOnePageView;
import views.admin.programs.ProgramNewOnePageViewModel;
import views.admin.programs.ProgramNewOneView;
import views.components.ToastMessage;

/** Controller for handling methods for admins managing program definitions. */
public final class AdminProgramController extends CiviFormController {

  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramIndexView listView;
  private final ProgramIndexPageView indexPageView;
  private final ProgramNewOneView newOneView;
  private final ProgramNewOnePageView newOnePageView;
  private final ProgramEditPageView editPageView;
  private final ProgramMetaDataEditView editView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final MessagesApi messagesApi;
  private final SettingsManifest settingsManifest;
  private final AccountRepository accountRepository;
  private final CategoryRepository categoryRepository;
  private final String baseUrl;

  @Inject
  public AdminProgramController(
      ProgramService programService,
      QuestionService questionService,
      ProgramIndexView listView,
      ProgramIndexPageView indexPageView,
      ProgramNewOneView newOneView,
      ProgramNewOnePageView newOnePageView,
      ProgramEditPageView editPageView,
      ProgramMetaDataEditView editView,
      VersionRepository versionRepository,
      ProfileUtils profileUtils,
      FormFactory formFactory,
      RequestChecker requestChecker,
      MessagesApi messagesApi,
      SettingsManifest settingsManifest,
      AccountRepository accountRepository,
      CategoryRepository categoryRepository,
      Config config) {
    super(profileUtils, versionRepository);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.listView = checkNotNull(listView);
    this.indexPageView = checkNotNull(indexPageView);
    this.newOneView = checkNotNull(newOneView);
    this.newOnePageView = checkNotNull(newOnePageView);
    this.editPageView = checkNotNull(editPageView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.messagesApi = checkNotNull(messagesApi);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.accountRepository = checkNotNull(accountRepository);
    this.categoryRepository = checkNotNull(categoryRepository);
    this.baseUrl = checkNotNull(config).getString("base_url");
  }

  /**
   * Returns an HTML page displaying all programs of the current live version and all programs of
   * the current draft version if any.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ActiveAndDraftPrograms programs = programService.getInUseActiveAndDraftPrograms();
      ProgramIndexPageViewModel model = buildIndexViewModel(request, programs, ProgramTab.IN_USE);
      return ok(indexPageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
    }
    Optional<CiviFormProfile> profileMaybe = profileUtils.optionalCurrentUserProfile(request);
    return ok(
        listView.render(
            programService.getInUseActiveAndDraftPrograms(),
            questionService.getReadOnlyQuestionServiceSync(),
            request,
            ProgramTab.IN_USE,
            profileMaybe));
  }

  /**
   * Returns an HTML page displaying all disabled programs of the current live version and all
   * disabled programs of the current draft version if any.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexDisabled(Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ProgramIndexPageViewModel model =
          buildIndexViewModel(
              request,
              programService.getDisabledActiveAndDraftProgramsWithoutQuestionLoad(),
              ProgramTab.DISABLED);
      return ok(indexPageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
    }
    Optional<CiviFormProfile> profileMaybe = profileUtils.optionalCurrentUserProfile(request);
    return ok(
        listView.render(
            programService.getDisabledActiveAndDraftProgramsWithoutQuestionLoad(),
            questionService.getReadOnlyQuestionServiceSync(),
            request,
            ProgramTab.DISABLED,
            profileMaybe));
  }

  /** Returns an HTML page containing a form to create a new program in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ProgramNewOnePageMapper mapper = new ProgramNewOnePageMapper();
      ProgramNewOnePageViewModel model =
          mapper.map(
              categoryRepository.listCategories(),
              accountRepository.listTrustedIntermediaryGroups(),
              settingsManifest.getExternalProgramCardsEnabled(),
              Optional.empty());
      return ok(newOnePageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
    }
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

    ImmutableList<ApplicationStep> applicationSteps =
        buildApplicationSteps(programData.getApplicationSteps());

    // Display any errors with the form input to the user.
    ImmutableSet<CiviFormError> errors =
        programService.validateProgramDataForCreate(
            programData.getAdminName(),
            programData.getLocalizedDisplayName(),
            programData.getLocalizedShortDescription(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            ImmutableList.copyOf(programData.getNotificationPreferences()),
            ImmutableList.copyOf(programData.getCategories()),
            ImmutableList.copyOf(programData.getTiGroups()),
            applicationSteps,
            ImmutableMap.of(),
            programData.getProgramType());
    if (!errors.isEmpty()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(errors));
      if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
        ProgramNewOnePageViewModel model =
            new ProgramNewOnePageMapper()
                .map(
                    categoryRepository.listCategories(),
                    accountRepository.listTrustedIntermediaryGroups(),
                    settingsManifest.getExternalProgramCardsEnabled(),
                    Optional.of("ERROR"));
        return ok(newOnePageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
      }
      return ok(newOneView.render(request, programData, message));
    }

    // If the user needs to confirm that they want to change the pre-screener form from a different
    // program to this one, show the confirmation dialog.
    if (programData.getProgramType().equals(ProgramType.PRE_SCREENER_FORM)
        && !programData.getConfirmedChangePreScreenerForm()) {
      Optional<ProgramDefinition> maybePreScreenerForm = programService.getPreScreenerForm();
      if (maybePreScreenerForm.isPresent()) {
        return ok(
            newOneView.renderChangePreScreenerConfirmation(
                request, programData, maybePreScreenerForm.get().localizedName().getDefault()));
      }
    }

    ErrorAnd<ProgramDefinition, CiviFormError> result =
        programService.createProgramDefinition(
            programData.getAdminName(),
            programData.getAdminDescription(),
            programData.getLocalizedDisplayName(),
            programData.getLocalizedDisplayDescription(),
            programData.getLocalizedShortDescription(),
            programData.getLocalizedConfirmationMessage(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            ImmutableList.copyOf(programData.getNotificationPreferences()),
            programData.getEligibilityIsGating(),
            programData.getLoginOnly(),
            programData.getProgramType(),
            ImmutableList.copyOf(programData.getTiGroups()),
            ImmutableList.copyOf(programData.getCategories()),
            applicationSteps,
            messagesApi.preferred(request),
            settingsManifest.getEnumeratorImprovementsEnabled(request));
    // There shouldn't be any errors since we already validated the program, but check for errors
    // again just in case.
    if (result.isError()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
      if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
        ProgramNewOnePageViewModel model =
            new ProgramNewOnePageMapper()
                .map(
                    categoryRepository.listCategories(),
                    accountRepository.listTrustedIntermediaryGroups(),
                    settingsManifest.getExternalProgramCardsEnabled(),
                    Optional.of("ERROR"));
        return ok(newOnePageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
      }
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
    ProgramEditStatus editStatusEnum = ProgramEditStatus.getStatusFromString(editStatus);

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ProgramEditPageMapper editMapper = new ProgramEditPageMapper();
      ProgramEditPageViewModel model =
          editMapper.map(
              program,
              editStatusEnum,
              categoryRepository.listCategories(),
              accountRepository.listTrustedIntermediaryGroups(),
              settingsManifest.getExternalProgramCardsEnabled(),
              baseUrl,
              Optional.empty());
      return ok(editPageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
    }
    return ok(editView.render(request, program, editStatusEnum));
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
          .flashing(FlashKey.ERROR, e.userFacingMessage());
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

    ImmutableList<ApplicationStep> applicationSteps =
        buildApplicationSteps(programData.getApplicationSteps());

    // Display any errors with the form input to the user.
    ImmutableSet<CiviFormError> validationErrors =
        programService.validateProgramDataForUpdate(
            programData.getLocalizedDisplayName(),
            programData.getLocalizedShortDescription(),
            programData.getExternalLink(),
            programData.getDisplayMode(),
            programData.getNotificationPreferences(),
            ImmutableList.copyOf(programData.getCategories()),
            ImmutableList.copyOf(programData.getTiGroups()),
            applicationSteps,
            programData.getProgramType());
    if (!validationErrors.isEmpty()) {
      ToastMessage message = ToastMessage.errorNonLocalized(joinErrors(validationErrors));
      if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
        ProgramEditPageViewModel model =
            new ProgramEditPageMapper()
                .map(
                    programDefinition,
                    programEditStatus,
                    categoryRepository.listCategories(),
                    accountRepository.listTrustedIntermediaryGroups(),
                    settingsManifest.getExternalProgramCardsEnabled(),
                    baseUrl,
                    Optional.of("ERROR"));
        return ok(editPageView.render(request, model)).as(play.mvc.Http.MimeTypes.HTML);
      }
      return ok(
          editView.render(request, programDefinition, programEditStatus, programData, message));
    }

    // If the user needs to confirm that they want to change the pre-screener form from a different
    // program to this one, show the confirmation dialog.
    if (programData.getProgramType().equals(ProgramType.PRE_SCREENER_FORM)
        && !programData.getConfirmedChangePreScreenerForm()) {
      Optional<ProgramDefinition> maybePreScreenerForm = programService.getPreScreenerForm();
      if (maybePreScreenerForm.isPresent()
          && !maybePreScreenerForm.get().adminName().equals(programDefinition.adminName())) {
        return ok(
            editView.renderChangePreScreenerConfirmation(
                request,
                programDefinition,
                programEditStatus,
                programData,
                maybePreScreenerForm.get().localizedName().getDefault()));
      }
    }

    programService.updateProgramDefinition(
        programDefinition.id(),
        LocalizedStrings.DEFAULT_LOCALE,
        programData.getAdminDescription(),
        programData.getLocalizedDisplayName(),
        programData.getLocalizedDisplayDescription(),
        programData.getLocalizedShortDescription(),
        programData.getLocalizedConfirmationMessage(),
        programData.getExternalLink(),
        programData.getDisplayMode(),
        programData.getNotificationPreferences(),
        programData.getEligibilityIsGating(),
        programData.getLoginOnly(),
        programData.getProgramType(),
        ImmutableList.copyOf(programData.getTiGroups()),
        ImmutableList.copyOf(programData.getCategories()),
        ImmutableList.copyOf(applicationSteps));
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

  /** Turn application step form data into ApplicationStep objects */
  ImmutableList<ApplicationStep> buildApplicationSteps(List<Map<String, String>> applicationSteps) {
    return applicationSteps.stream()
        .filter(
            step -> {
              // include the step if either the title or description is filled out
              boolean haveKeys = step.containsKey("title") && step.containsKey("description");
              return haveKeys
                  && (!step.get("title").isBlank() || !step.get("description").isBlank());
            })
        .map(
            step -> {
              return new ApplicationStep(step.get("title"), step.get("description"));
            })
        .collect(ImmutableList.toImmutableList());
  }

  private ProgramIndexPageViewModel buildIndexViewModel(
      Request request, ActiveAndDraftPrograms programs, ProgramTab selectedTab) {
    String civicEntityName = settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("");

    ActiveAndDraftQuestions questions =
        questionService.getReadOnlyQuestionServiceSync().getActiveAndDraftQuestions();

    ImmutableList<Long> universalQuestionIds =
        questionService.getReadOnlyQuestionServiceSync().getUpToDateQuestions().stream()
            .filter(QuestionDefinition::isUniversal)
            .map(QuestionDefinition::getId)
            .collect(ImmutableList.toImmutableList());

    ActiveAndDraftPrograms allPrograms =
        programService.getActiveAndDraftProgramsWithoutQuestionLoad();

    ImmutableList<QuestionDefinition> draftQuestions =
        questionService
            .getReadOnlyQuestionServiceSync()
            .getActiveAndDraftQuestions()
            .getDraftQuestions();

    ProgramIndexPageMapper indexMapper = new ProgramIndexPageMapper();
    return indexMapper.map(
        programs,
        questions,
        selectedTab,
        civicEntityName,
        universalQuestionIds,
        allPrograms,
        draftQuestions,
        baseUrl,
        programService,
        request.flash().get(FlashKey.SUCCESS),
        request.flash().get(FlashKey.ERROR));
  }
}
