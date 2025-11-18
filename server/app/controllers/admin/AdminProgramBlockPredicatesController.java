package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.BlockEligibilityMessageForm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Builder;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.EligibilityNotValidForProgramTypeException;
import services.program.IllegalPredicateOrderingException;
import services.program.PredicateDefinitionNotFoundException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.predicate.Operator;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateUseCase;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;
import views.admin.programs.predicates.AddFirstConditionPartialView;
import views.admin.programs.predicates.AddFirstConditionPartialViewModel;
import views.admin.programs.predicates.DeleteConditionCommand;
import views.admin.programs.predicates.EditConditionCommand;
import views.admin.programs.predicates.EditConditionPartialView;
import views.admin.programs.predicates.EditConditionPartialViewModel;
import views.admin.programs.predicates.EditPredicatePageView;
import views.admin.programs.predicates.EditPredicatePageViewModel;
import views.admin.programs.predicates.EditSubconditionCommand;
import views.admin.programs.predicates.EditSubconditionPartialView;
import views.admin.programs.predicates.EditSubconditionPartialViewModel;
import views.admin.programs.predicates.FailedRequestPartialView;
import views.admin.programs.predicates.FailedRequestPartialViewModel;
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
  private final EditSubconditionPartialView editSubconditionPartialView;
  private final FailedRequestPartialView failedRequestPartialView;
  private final AddFirstConditionPartialView addFirstConditionPartialView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final SettingsManifest settingsManifest;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

  /**
   * Value to track how many predicates are currently present on the page. Keys are conditionIds,
   * values are top-level predicates.
   */
  private final Map<Long, EditConditionPartialViewModel> topLevelConditions = new HashMap<>();

  /**
   * Contains data for rendering a simple HTML option element with no additional data attributes.
   */
  @Builder
  public record OptionElement(String value, String displayText, boolean selected) {}

  /**
   * Contains data for rendering an HTML option element with additional data attributes required for
   * {@link Scalar} options.
   */
  @Builder
  public record ScalarOptionElement(
      String value, String displayText, String scalarType, boolean selected) {}

  @Inject
  public AdminProgramBlockPredicatesController(
      PredicateGenerator predicateGenerator,
      ProgramService programService,
      QuestionService questionService,
      ProgramPredicatesEditView legacyPredicatesEditView,
      ProgramPredicateConfigureView legacyPredicatesConfigureView,
      EditPredicatePageView editPredicatePageView,
      EditConditionPartialView editConditionPartialView,
      EditSubconditionPartialView editSubconditionPartialView,
      FailedRequestPartialView failedRequestPartialView,
      AddFirstConditionPartialView addFirstConditionPartialView,
      FormFactory formFactory,
      RequestChecker requestChecker,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.predicateGenerator = checkNotNull(predicateGenerator);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.legacyPredicatesEditView = checkNotNull(legacyPredicatesEditView);
    this.legacyPredicatesConfigureView = checkNotNull(legacyPredicatesConfigureView);
    this.editPredicatePageView = checkNotNull(editPredicatePageView);
    this.editConditionPartialView = checkNotNull(editConditionPartialView);
    this.editSubconditionPartialView = checkNotNull(editSubconditionPartialView);
    this.failedRequestPartialView = checkNotNull(failedRequestPartialView);
    this.addFirstConditionPartialView = checkNotNull(addFirstConditionPartialView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
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
        this.topLevelConditions.clear();
        EditPredicatePageViewModel model =
            EditPredicatePageViewModel.builder()
                .programDefinition(programDefinition)
                .blockDefinition(blockDefinition)
                .predicateUseCase(predicateUseCase)
                .operatorScalarMap(getOperatorScalarMap())
                .currentAddedConditions(ImmutableList.copyOf(this.topLevelConditions.values()))
                .hasAvailableQuestions(!predicateQuestions.isEmpty())
                .build();
        return ok(editPredicatePageView.render(request, model)).as(Http.MimeTypes.HTML);
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
      long programId, long blockDefinitionId, PredicateUseCase predicateUseCase)
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
    return getAvailablePredicateQuestionDefinitions(
        programDefinition, blockDefinitionId, predicateUseCase);
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

  /**
   * Creates a map of {@link Operater} name to a list of {@link Scalar} names that the operator can
   * be used with. This is used on the client for filtering operator options based on a selected
   * scalar.
   */
  @VisibleForTesting
  static ImmutableMap<String, ImmutableList<String>> getOperatorScalarMap() {
    return Arrays.stream(Operator.values())
        .collect(
            ImmutableMap.toImmutableMap(
                Operator::name,
                operator ->
                    operator.getOperableTypes().stream()
                        .map(Enum::name)
                        .collect(ImmutableList.toImmutableList())));
  }

  /**
   * POST endpoint for updating show-hide configurations.
   *
   * <p>TODO(#11764): Clean this up once expanded form logic is fully rolled out and this endpoint
   * is unused in favor of updatePredicate.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      PredicateDefinition predicateDefinition =
          predicateGenerator.legacyGeneratePredicateDefinition(
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

  /**
   * POST endpoint for updating eligibility configurations. TODO(#11764): Clean this up once
   * expanded form logic is fully rolled out and this endpoint is unused in favor of
   * updatePredicate.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateEligibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      EligibilityDefinition eligibility =
          EligibilityDefinition.builder()
              .setPredicate(
                  predicateGenerator.legacyGeneratePredicateDefinition(
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

  /** POST endpoint for updating predicates. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updatePredicate(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      PredicateDefinition predicateDefinition =
          predicateGenerator.generatePredicateDefinition(
              programService.getFullProgramDefinition(programId),
              formFactory.form().bindFromRequest(request),
              roQuestionService,
              settingsManifest,
              request);

      switch (PredicateUseCase.valueOf(predicateUseCase)) {
        case ELIGIBILITY ->
            programService.setBlockEligibilityDefinition(
                programId,
                blockDefinitionId,
                Optional.of(
                    EligibilityDefinition.builder().setPredicate(predicateDefinition).build()));
        case VISIBILITY ->
            programService.setBlockVisibilityPredicate(
                programId, blockDefinitionId, Optional.of(predicateDefinition));
      }
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException
        | QuestionNotFoundException
        | ProgramQuestionDefinitionNotFoundException
        | EligibilityNotValidForProgramTypeException
        | BadRequestException e) {
      // TODO(#11761): Replace toast with dismissable alert when admin alerts are ready.
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    // TODO(#11761): Replace toast with dismissable alert when admin alerts are ready.
    return redirect(routes.AdminProgramBlocksController.edit(programId, blockDefinitionId))
        .flashing(
            FlashKey.SUCCESS,
            String.format("Saved %s condition", predicateUseCase.toLowerCase(Locale.ROOT)));
  }

  /** HTMX partial that renders a card for editing a condition within a predicate. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxEditCondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    Form<EditConditionCommand> form =
        formFactory.form(EditConditionCommand.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
      ImmutableList<QuestionDefinition> availableQuestions =
          getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, useCase);
      if (availableQuestions.isEmpty()) {
        // TODO(#11617): Render alert with message that there are no available questions.
        return notFound();
      }
      Long conditionId = form.get().getConditionId();
      EditConditionPartialViewModel condition =
          EditConditionPartialViewModel.builder()
              .programId(programId)
              .blockId(blockDefinitionId)
              .predicateUseCase(useCase)
              .conditionId(conditionId)
              .selectedQuestionType(Optional.empty())
              .questionOptions(
                  getQuestionOptions(availableQuestions, /* selectedQuestion= */ Optional.empty()))
              .scalarOptions(ImmutableList.of())
              .operatorOptions(getOperatorOptions())
              .build();
      condition =
          condition.toBuilder()
              .subconditions(ImmutableList.of(condition.emptySubconditionViewModel()))
              .build();

      // Update controller predicate info.
      this.topLevelConditions.put(conditionId, condition);
      return ok(editConditionPartialView.render(request, condition)).as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /**
   * HTMX partial that renders a form for editing a subcondition within a condition of a predicate.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxEditSubcondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }
    Form<EditSubconditionCommand> form =
        formFactory.form(EditSubconditionCommand.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
      ImmutableList<QuestionDefinition> availableQuestions =
          getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, useCase);
      if (availableQuestions.isEmpty()) {
        // TODO(#11617): Render alert with message that there are no available questions.
        return notFound();
      }

      long conditionId = form.get().getConditionId();
      long subconditionId = form.get().getSubconditionId();
      Optional<QuestionDefinition> selectedQuestion =
          getSelectedQuestion(request, conditionId, subconditionId, availableQuestions);
      EditSubconditionPartialViewModel subcondition =
          EditSubconditionPartialViewModel.builder()
              .programId(programId)
              .blockId(blockDefinitionId)
              .predicateUseCase(useCase)
              .conditionId(conditionId)
              .subconditionId(subconditionId)
              .selectedQuestionType(
                  selectedQuestion.map(question -> question.getQuestionType().getLabel()))
              .questionOptions(getQuestionOptions(availableQuestions, selectedQuestion))
              .scalarOptions(
                  selectedQuestion
                      .map(question -> getScalarOptionsForQuestion(question))
                      .orElse(ImmutableList.of()))
              .operatorOptions(getOperatorOptions())
              .valueOptions(
                  selectedQuestion
                      .map(question -> getValueOptionsForQuestion(question))
                      .orElse(ImmutableList.of()))
              .build();

      // Update subconditions map with new subcondition.
      EditConditionPartialViewModel condition = this.topLevelConditions.get(conditionId);

      if (condition == null) {
        throw new PredicateDefinitionNotFoundException(programId, blockDefinitionId, conditionId);
      }

      ArrayList<EditSubconditionPartialViewModel> subconditionsList =
          new ArrayList<>(condition.subconditions());

      if (subconditionsList.size() < subconditionId) {
        subconditionsList.add(subcondition);
      } else {
        subconditionsList.set((int) subconditionId - 1, subcondition);
      }
      condition =
          condition.toBuilder().subconditions(ImmutableList.copyOf(subconditionsList)).build();
      this.topLevelConditions.put(conditionId, condition);

      return ok(editSubconditionPartialView.render(request, subcondition)).as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | PredicateDefinitionNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /**
   * HTMX endpoint that re-renders predicate conditions, dropping the condition with id idToDelete
   * from the DOM.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxDeleteCondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    String returnedHTML = " ";
    Map<Long, EditConditionPartialViewModel> newTopLevelConditions = new HashMap<>();

    Form<DeleteConditionCommand> form =
        formFactory.form(DeleteConditionCommand.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    Long conditionToDelete = form.get().getConditionId();

    for (Entry<Long, EditConditionPartialViewModel> entry : this.topLevelConditions.entrySet()) {
      Long conditionId = entry.getKey();
      EditConditionPartialViewModel condition = entry.getValue();
      ArrayList<EditSubconditionPartialViewModel> subconditionsList =
          new ArrayList<>(condition.subconditions());
      try {
        if (this.topLevelConditions.size() == 1) {
          PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
          // If this is the only condition, we should just render a new AddCondition partial.
          returnedHTML =
              returnedHTML.concat(
                  addFirstConditionPartialView.render(
                      request,
                      AddFirstConditionPartialViewModel.builder()
                          .programId(programId)
                          .blockId(blockDefinitionId)
                          .predicateUseCase(useCase)
                          .build()));
        } else if (conditionId.equals(conditionToDelete)) {
          // This is the condition we're supposed to delete. Do nothing and continue.
        } else if (conditionId > conditionToDelete) {
          // For conditions past the deleted ID, change their conditionId and re-render.
          // Also update subconditions with new conditionId.
          Long newConditionId = conditionId - 1L;
          ArrayList<EditSubconditionPartialViewModel> newSubconditionsList =
              subconditionsList.stream()
                  .map(subcondition -> subcondition.toBuilder().conditionId(newConditionId).build())
                  .collect(Collectors.toCollection(ArrayList::new));
          EditConditionPartialViewModel newCondition =
              condition.toBuilder()
                  .conditionId(newConditionId)
                  .subconditions(ImmutableList.copyOf(newSubconditionsList))
                  .disableRenderAddCondition(true)
                  .build();

          // Render the addCondition fragment if this is the last condition.
          if (conditionId == this.topLevelConditions.size()) {
            newCondition = newCondition.toBuilder().disableRenderAddCondition(false).build();
          }

          returnedHTML =
              returnedHTML
                  .concat("\n")
                  .concat(editConditionPartialView.render(request, newCondition));

          // Update controller info for this condition.
          newTopLevelConditions.put(newConditionId, newCondition);
        } else {
          // For conditions before the deleted conditionId, re-render as-is.
          EditConditionPartialViewModel refreshedCondition =
              condition.toBuilder()
                  .subconditions(ImmutableList.copyOf(subconditionsList))
                  .disableRenderAddCondition(true)
                  .build();

          // Render the addCondition fragment if this is going to be the last condition after the
          // last condition is deleted.
          Long maxConditionId =
              this.topLevelConditions.entrySet().stream()
                  .max(Comparator.comparingLong(Entry::getKey))
                  .orElseThrow(() -> new IllegalArgumentException("Expected max conditionId."))
                  .getKey();
          if (conditionToDelete.equals(maxConditionId) && conditionId.equals(maxConditionId - 1L)) {
            refreshedCondition =
                refreshedCondition.toBuilder().disableRenderAddCondition(false).build();
          }

          returnedHTML =
              returnedHTML
                  .concat("\n")
                  .concat(editConditionPartialView.render(request, refreshedCondition));

          // Still need to add the controller info here so that it's persisted.
          newTopLevelConditions.put(conditionId, condition);
        }
      } catch (IllegalArgumentException e) {
        return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
            .as(Http.MimeTypes.HTML);
      }
    }

    // Clear all old condition and subcondition data. We'll replace it with the new data below.
    this.topLevelConditions.clear();

    // Repopulate top-level conditions with new info.
    for (Entry<Long, EditConditionPartialViewModel> newEntry : newTopLevelConditions.entrySet()) {
      Long newConditionId = newEntry.getKey();
      EditConditionPartialViewModel newCondition = newEntry.getValue();
      this.topLevelConditions.put(newConditionId, newCondition);
    }

    return ok(returnedHTML).as(Http.MimeTypes.HTML);
  }

  /**
   * Get the selected question from the request. Returns an empty optional if there is none or if it
   * can't be parsed.
   */
  private Optional<QuestionDefinition> getSelectedQuestion(
      Request request,
      long conditionId,
      long subconditionId,
      ImmutableList<QuestionDefinition> availableQuestions) {
    DynamicForm formData = formFactory.form().bindFromRequest(request);
    String questionId =
        formData.get(
            String.format("condition-%d-subcondition-%d-question", conditionId, subconditionId));
    Optional<Long> selectedQuestionId = Optional.empty();
    if (questionId != null) {
      try {
        selectedQuestionId = Optional.of(Long.parseLong(questionId));
      } catch (NumberFormatException e) {
        // continue with empty optional
      }
    }
    return selectedQuestionId.flatMap(
        id -> availableQuestions.stream().filter(q -> q.getId() == id).findFirst());
  }

  /**
   * Converts a list of {@link QuestionDefinition}s to a list of {@link OptionElement}s for use in a
   * select dropdown.
   */
  private ImmutableList<OptionElement> getQuestionOptions(
      ImmutableList<QuestionDefinition> availableQuestions,
      Optional<QuestionDefinition> selectedQuestion) {
    ImmutableList.Builder<OptionElement> questionOptions = new ImmutableList.Builder<>();
    for (QuestionDefinition question : availableQuestions) {
      questionOptions.add(
          OptionElement.builder()
              .value(String.valueOf(question.getId()))
              .displayText(question.getQuestionText().getDefault())
              .selected(
                  selectedQuestion.isPresent()
                      && question.getId() == selectedQuestion.get().getId())
              .build());
    }
    return questionOptions.build();
  }

  /**
   * Returns a list of {@link ScalarOptionElement}s for the given question with the first option
   * selected by default.
   */
  private ImmutableList<ScalarOptionElement> getScalarOptionsForQuestion(
      QuestionDefinition question) {
    ImmutableList<Scalar> scalars = ImmutableList.of();
    if (question.isAddress()) {
      scalars = ImmutableList.of(Scalar.SERVICE_AREAS);
    } else if (question.getQuestionType().equals(QuestionType.NAME)) {
      // Name suffix is not included in predicates.
      scalars = ImmutableList.of(Scalar.FIRST_NAME, Scalar.MIDDLE_NAME, Scalar.LAST_NAME);
    } else {
      try {
        scalars = Scalar.getScalars(question.getQuestionType()).asList();
      } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
        // This should never happen since we filter out Enumerator questions before this point.
        return ImmutableList.of();
      }
    }
    ImmutableList.Builder<ScalarOptionElement> scalarOptionsBuilder = new ImmutableList.Builder<>();
    AtomicBoolean isFirst = new AtomicBoolean(true);
    scalars.forEach(
        scalar ->
            scalarOptionsBuilder.add(
                ScalarOptionElement.builder()
                    .value(scalar.name())
                    .displayText(scalar.toDisplayString())
                    .scalarType(scalar.toScalarType().name())
                    .selected(isFirst.getAndSet(false))
                    .build()));

    return scalarOptionsBuilder.build();
  }

  /**
   * Returns a list of {@link OptionElement}s representing all possible Values for the condition,
   * depending on question type.
   */
  private ImmutableList<OptionElement> getValueOptionsForQuestion(QuestionDefinition question) {
    AtomicBoolean isFirst = new AtomicBoolean(true);
    if (question.isAddress()) {
      return esriServiceAreaValidationConfig.getImmutableMap().entrySet().stream()
          .map(
              entry -> {
                return OptionElement.builder()
                    .value(entry.getKey())
                    .displayText(entry.getValue().getLabel())
                    .selected(isFirst.getAndSet(false))
                    .build();
              })
          .collect(ImmutableList.toImmutableList());
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * Returns a list of {@link OptionElement}s representing all possible {@link Operator}s. These
   * will be filtered and marked hidden on the client based on the associated {@link Scalar}
   * dropdown.
   */
  private ImmutableList<OptionElement> getOperatorOptions() {
    return Arrays.stream(Operator.values())
        .map(
            operator -> {
              return OptionElement.builder()
                  .value(operator.name())
                  .displayText(operator.toDisplayString())
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }
}
