package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.BlockEligibilityMessageForm;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.EligibilityNotValidForProgramTypeException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateUseCase;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;
import views.admin.programs.predicates.EditConditionCommand;
import views.admin.programs.predicates.EditConditionPartialView;
import views.admin.programs.predicates.EditConditionPartialViewModel;
import views.admin.programs.predicates.EditPredicatePageView;
import views.admin.programs.predicates.EditPredicatePageViewModel;
import views.components.ToastMessage;
import views.components.ToastMessage.ToastType;

/**
 * Controller for admins editing and viewing program predicates for eligibility and visibility
 * logic.
 */
public class AdminProgramBlockPredicatesController extends CiviFormController {
  private final PredicateGenerator predicateGenerator;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramPredicatesEditView legacyPredicatesEditView;
  private final ProgramPredicateConfigureView legacyPredicatesConfigureView;
  private final EditPredicatePageView editPredicatePageView;
  private final EditConditionPartialView editConditionPartialView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminProgramBlockPredicatesController(
      PredicateGenerator predicateGenerator,
      ProgramService programService,
      QuestionService questionService,
      ProgramPredicatesEditView legacyPredicatesEditView,
      ProgramPredicateConfigureView legacyPredicatesConfigureView,
      EditPredicatePageView editPredicatePageView,
      EditConditionPartialView editConditionPartialView,
      FormFactory formFactory,
      RequestChecker requestChecker,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.predicateGenerator = checkNotNull(predicateGenerator);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.legacyPredicatesEditView = checkNotNull(legacyPredicatesEditView);
    this.legacyPredicatesConfigureView = checkNotNull(legacyPredicatesConfigureView);
    this.editPredicatePageView = checkNotNull(editPredicatePageView);
    this.editConditionPartialView = checkNotNull(editConditionPartialView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Return an HTML page containing current show-hide configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editVisibility(Request request, long programId, long blockDefinitionId) {
    return editPredicate(request, programId, blockDefinitionId, PredicateUseCase.VISIBILITY);
  }

  /**
   * Return an HTML page containing current eligibility configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editEligibility(Request request, long programId, long blockDefinitionId) {
    return editPredicate(request, programId, blockDefinitionId, PredicateUseCase.ELIGIBILITY);
  }

  private Result editPredicate(
      Request request, long programId, long blockDefinitionId, PredicateUseCase predicateUseCase) {
    requestChecker.throwIfProgramNotDraft(programId);
    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);
      ImmutableList<QuestionDefinition> predicateQuestions =
          getAvailablePredicateQuestionDefinitions(
              programDefinition, blockDefinitionId, predicateUseCase);

      if (settingsManifest.getExpandedFormLogicEnabled(request)) {
        return ok(editPredicatePageView.render(
                request,
                new EditPredicatePageViewModel(
                    programDefinition, blockDefinition, predicateUseCase, predicateQuestions)))
            .as(Http.MimeTypes.HTML);
      }

      return ok(
          legacyPredicatesEditView.render(
              request, programDefinition, blockDefinition, predicateQuestions, predicateUseCase));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  private ImmutableList<QuestionDefinition> getAvailablePredicateQuestionDefinitions(
      ProgramDefinition programDefinition,
      long blockDefinitionId,
      PredicateUseCase predicateUseCase)
      throws ProgramBlockDefinitionNotFoundException {
    return switch (predicateUseCase) {
      case ELIGIBILITY ->
          programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(blockDefinitionId);
      case VISIBILITY ->
          programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(blockDefinitionId);
    };
  }

  /** POST endpoint for updating show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      PredicateDefinition predicateDefinition =
          predicateGenerator.generatePredicateDefinition(
              programService.getFullProgramDefinition(programId),
              formFactory.form().bindFromRequest(request),
              roQuestionService);

      programService.setBlockVisibilityPredicate(
          programId, blockDefinitionId, Optional.of(predicateDefinition));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException
        | QuestionNotFoundException
        | ProgramQuestionDefinitionNotFoundException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editVisibility(
                  programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editVisibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Saved visibility condition");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureNewVisibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          legacyPredicatesConfigureView.renderNewVisibility(
              request,
              programDefinition,
              blockDefinition,
              getQuestionDefinitionsForForm(programDefinition, blockDefinition, form)));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureExistingVisibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      ImmutableList<Long> visibilityQuestionIds =
          blockDefinition
              .visibilityPredicate()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          String.format(
                              "Block %d has no visibility predicate", blockDefinition.id())))
              .getQuestions();

      ImmutableList<QuestionDefinition> visibilityQuestionDefinitions =
          getQuestionDefinitions(
              programDefinition,
              blockDefinition,
              /* selectionPredicate= */ (QuestionDefinition questionDefinition) ->
                  visibilityQuestionIds.contains(questionDefinition.getId()));

      return ok(
          legacyPredicatesConfigureView.renderExistingVisibility(
              request, programDefinition, blockDefinition, visibilityQuestionDefinitions));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureNewEligibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          legacyPredicatesConfigureView.renderNewEligibility(
              request,
              programDefinition,
              blockDefinition,
              getQuestionDefinitionsForForm(programDefinition, blockDefinition, form)));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureExistingEligibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      ImmutableList<Long> eligibilityQuestionIds =
          blockDefinition
              .eligibilityDefinition()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          String.format(
                              "Block %d has no eligibility definition", blockDefinition.id())))
              .predicate()
              .getQuestions();

      ImmutableList<QuestionDefinition> eligibilityQuestionDefinitions =
          getQuestionDefinitions(
              programDefinition,
              blockDefinition,
              /* selectionPredicate= */ (QuestionDefinition questionDefinition) ->
                  eligibilityQuestionIds.contains(questionDefinition.getId()));

      return ok(
          legacyPredicatesConfigureView.renderExistingEligibility(
              request, programDefinition, blockDefinition, eligibilityQuestionDefinitions));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private ImmutableList<QuestionDefinition> getQuestionDefinitionsForForm(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition, DynamicForm form) {
    return getQuestionDefinitions(
        programDefinition,
        blockDefinition,
        (QuestionDefinition questionDefinition) ->
            form.rawData().containsKey(String.format("question-%d", questionDefinition.getId())));
  }

  private ImmutableList<QuestionDefinition> getQuestionDefinitions(
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      Predicate<QuestionDefinition> selectionPredicate) {

    try {
      return programDefinition
          .getAvailablePredicateQuestionDefinitions(blockDefinition.id())
          .stream()
          .filter(selectionPredicate)
          .collect(ImmutableList.toImmutableList());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** POST endpoint for updating eligibility configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateEligibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      EligibilityDefinition eligibility =
          EligibilityDefinition.builder()
              .setPredicate(
                  predicateGenerator.generatePredicateDefinition(
                      programService.getFullProgramDefinition(programId),
                      formFactory.form().bindFromRequest(request),
                      roQuestionService))
              .build();

      programService.setBlockEligibilityDefinition(
          programId, blockDefinitionId, Optional.of(eligibility));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException
        | QuestionNotFoundException
        | ProgramQuestionDefinitionNotFoundException
        | EligibilityNotValidForProgramTypeException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editEligibility(
                  programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editEligibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Saved eligibility condition");
  }

  /** POST endpoint for deleting show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroyVisibility(long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeBlockPredicate(programId, blockDefinitionId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editVisibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Removed the visibility condition for this screen.");
  }

  /** POST endpoint for deleting eligibility configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroyEligibility(long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeBlockEligibilityPredicate(programId, blockDefinitionId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editEligibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Removed the eligibility condition for this screen.");
  }

  /** POST endpoint for updating eligibility message. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateEligibilityMessage(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    Form<BlockEligibilityMessageForm> EligibilityMsgform =
        formFactory.form(BlockEligibilityMessageForm.class).bindFromRequest(request);
    String newMessage = EligibilityMsgform.get().getEligibilityMessage();
    String toastMessage;
    ToastType toastType;

    try {
      programService.setBlockEligibilityMessage(
          programId, blockDefinitionId, Optional.of(LocalizedStrings.of(Locale.US, newMessage)));

      toastType = ToastMessage.ToastType.SUCCESS;
      if (newMessage.isBlank()) {
        toastMessage = "Eligibility message removed.";
      } else {
        toastMessage = "Eligibility message set to " + newMessage;
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    }
    final String indexUrl =
        routes.AdminProgramBlockPredicatesController.editEligibility(programId, blockDefinitionId)
            .url();
    return redirect(indexUrl)
        .flashing(toastType.toString().toLowerCase(Locale.getDefault()), toastMessage);
  }

  /** HTMX partial that renders a card for editing a condition within a predicate. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxEditCondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCaseString) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    Form<EditConditionCommand> form =
        formFactory.form(EditConditionCommand.class).bindFromRequest(request);
    if (form.hasErrors()) {
      // TODO(#11560): Render error alert.
    }

    PredicateUseCase predicateUseCase = PredicateUseCase.valueOf(predicateUseCaseString);
    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      DynamicForm formData = formFactory.form().bindFromRequest(request);
      long conditionId = Long.parseLong(formData.get("conditionId"));
      ImmutableList<QuestionDefinition> predicateQuestions =
          getAvailablePredicateQuestionDefinitions(
              programDefinition, blockDefinitionId, predicateUseCase);
      return ok(editConditionPartialView.render(
              request,
              new EditConditionPartialViewModel(
                  programId, blockDefinitionId, predicateUseCase, conditionId, predicateQuestions)))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      // TODO(#11560): Render error alert.
      return notFound();
    }
  }
}
