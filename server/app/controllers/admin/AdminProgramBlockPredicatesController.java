package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.BadRequestException;
import controllers.CiviFormController;
import controllers.FlashKey;
import controllers.admin.AdminProgramBlockPredicatesController.OptionElement;
import controllers.admin.AdminProgramBlockPredicatesController.ScalarOptionElement;
import forms.admin.BlockEligibilityMessageForm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
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
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;
import views.admin.programs.predicates.ConditionListPartialView;
import views.admin.programs.predicates.ConditionListPartialViewModel;
import views.admin.programs.predicates.EditConditionPartialViewModel;
import views.admin.programs.predicates.EditPredicatePageView;
import views.admin.programs.predicates.EditPredicatePageViewModel;
import views.admin.programs.predicates.EditSubconditionPartialViewModel;
import views.admin.programs.predicates.EditSubconditionPartialViewModel.EditSubconditionPartialViewModelBuilder;
import views.admin.programs.predicates.FailedRequestPartialView;
import views.admin.programs.predicates.FailedRequestPartialViewModel;
import views.admin.programs.predicates.SubconditionListPartialView;
import views.admin.programs.predicates.SubconditionListPartialViewModel;
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
  private final FailedRequestPartialView failedRequestPartialView;
  private final ConditionListPartialView conditionListPartialView;
  private final SubconditionListPartialView subconditionListPartialView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final SettingsManifest settingsManifest;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;

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
      FailedRequestPartialView failedRequestPartialView,
      ConditionListPartialView conditionListPartialView,
      SubconditionListPartialView subconditionListPartialView,
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
    this.failedRequestPartialView = checkNotNull(failedRequestPartialView);
    this.conditionListPartialView = checkNotNull(conditionListPartialView);
    this.subconditionListPartialView = checkNotNull(subconditionListPartialView);
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
        EditPredicatePageViewModel model =
            EditPredicatePageViewModel.builder()
                .programDefinition(programDefinition)
                .blockDefinition(blockDefinition)
                .predicateUseCase(predicateUseCase)
                .operatorScalarMap(getOperatorScalarMap())
                .prePopulatedConditions(ImmutableList.of())
                .hasAvailableQuestions(!predicateQuestions.isEmpty())
                .eligibilityMessage(
                    blockDefinition
                        .localizedEligibilityMessage()
                        .map(LocalizedStrings::getDefault)
                        .orElse(""))
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
      DynamicForm form = formFactory.form().bindFromRequest(request);
      PredicateDefinition predicateDefinition =
          predicateGenerator.generatePredicateDefinition(
              programService.getFullProgramDefinition(programId),
              form,
              roQuestionService,
              settingsManifest,
              request);
      if (predicateDefinition.getQuestions().isEmpty()) {
        // If there are no questions in the predicate, that means there are no conditions and we
        // should remove the predicate.
        switch (PredicateUseCase.valueOf(predicateUseCase)) {
          case ELIGIBILITY ->
              programService.removeBlockEligibilityPredicate(programId, blockDefinitionId);
          case VISIBILITY -> programService.removeBlockPredicate(programId, blockDefinitionId);
        }
      } else {
        // Otherwise, update the predicate with the new definition.
        switch (PredicateUseCase.valueOf(predicateUseCase)) {
          case ELIGIBILITY -> {
            programService.setBlockEligibilityDefinition(
                programId,
                blockDefinitionId,
                Optional.of(
                    EligibilityDefinition.builder().setPredicate(predicateDefinition).build()));
          }
          case VISIBILITY ->
              programService.setBlockVisibilityPredicate(
                  programId, blockDefinitionId, Optional.of(predicateDefinition));
        }
      }
      // Update eligibility message if provided.
      if (PredicateUseCase.valueOf(predicateUseCase) == PredicateUseCase.ELIGIBILITY
          && form.rawData().containsKey("eligibilityMessage")) {
        programService.setBlockEligibilityMessage(
            programId,
            blockDefinitionId,
            Optional.of(LocalizedStrings.of(Locale.US, form.get("eligibilityMessage"))));
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
      // TODO(#11761): Replace toast with dismissable alert when admin alerts are
      // ready.
      return redirect(routes.AdminProgramBlocksController.edit(programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    // TODO(#11761): Replace toast with dismissable alert when admin alerts are
    // ready.
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

    DynamicForm form = formFactory.form().bindFromRequest(request);
    if (form.rawData().get("conditionId") == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
      ImmutableList<QuestionDefinition> availableQuestions =
          getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, useCase);
      if (availableQuestions.isEmpty()) {
        // TODO(#11617): Render alert with message that there are no available
        // questions.
        return notFound();
      }

      ArrayList<EditConditionPartialViewModel> currentConditions =
          new ArrayList<>(
              buildConditionsListFromFormData(
                  programId,
                  blockDefinitionId,
                  predicateUseCase,
                  ImmutableMap.copyOf(form.rawData())));
      EditConditionPartialViewModel condition =
          EditConditionPartialViewModel.builder()
              .programId(programId)
              .blockId(blockDefinitionId)
              .predicateUseCase(useCase)
              .questionOptions(
                  getQuestionOptions(availableQuestions, /* selectedQuestion= */ Optional.empty()))
              .scalarOptions(ImmutableList.of())
              .operatorOptions(getOperatorOptions())
              .build();
      condition =
          condition.toBuilder()
              .subconditions(ImmutableList.of(condition.emptySubconditionViewModel()))
              .build();
      currentConditions.add(condition);

      // Render an updated list of conditions (sorted by conditionId).
      return ok(conditionListPartialView.render(
              request,
              ConditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(useCase)
                  .conditions(ImmutableList.copyOf(currentConditions))
                  .build()))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /**
   * HTMX partial that renders a form for editing a subcondition within a condition of a predicate.
   *
   * <p>Used to update the subcondition form when a question is changed, and to add an empty
   * subcondition.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxEditSubcondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }
    DynamicForm form = formFactory.form().bindFromRequest(request);
    if (form.hasErrors() || form.get("conditionId") == null || form.get("subconditionId") == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      Map<String, String> formData = new HashMap<>(form.rawData());
      long conditionId = Long.valueOf(formData.get("conditionId"));
      long subconditionId = Long.valueOf(form.get("subconditionId"));

      // Dynamic forms contain full form from the request.
      // We need to start by filtering to only this condition.
      String parentCondition = String.format("condition-%d", conditionId);
      formData.keySet().removeIf(key -> !key.startsWith(parentCondition));

      // If the user is editing a subcondition, the built condition list will contain those edits.
      EditConditionPartialViewModel condition =
          getOnlyElement(
              buildConditionsListFromFormData(
                  programId, blockDefinitionId, predicateUseCase, ImmutableMap.copyOf(formData)));
      ArrayList<EditSubconditionPartialViewModel> subconditionList =
          new ArrayList<>(condition.subconditions());

      // Create a new subcondition if it's not pre-existing.
      boolean isNewSubcondition =
          !formData.keySet().stream()
              .anyMatch(
                  key ->
                      key.startsWith(
                          String.format(
                              "condition-%d-subcondition-%d", conditionId, subconditionId)));
      if (isNewSubcondition) {
        subconditionList.add(condition.emptySubconditionViewModel());
      }

      return ok(subconditionListPartialView.render(
              request,
              SubconditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .conditionId(conditionId)
                  .subconditions(ImmutableList.copyOf(subconditionList))
                  .build()))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /**
   * HTMX endpoint that re-renders predicate conditions, dropping the condition with id conditionId
   * from the DOM.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxDeleteCondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    DynamicForm form = formFactory.form().bindFromRequest(request);
    String idToRemove = form.rawData().get("conditionId");
    if (form.hasErrors() || idToRemove == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      String removedConditionPrefix = "condition-" + idToRemove;
      Map<String, String> formData = new HashMap<>(form.rawData());

      // Start by pre-filtering formData to remove entry for the deleted condition.
      formData.keySet().removeIf(key -> key.startsWith(removedConditionPrefix));

      ImmutableList<EditConditionPartialViewModel> conditions =
          buildConditionsListFromFormData(
              programId, blockDefinitionId, predicateUseCase, ImmutableMap.copyOf(formData));

      return ok(conditionListPartialView.render(
              request,
              ConditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .conditions(conditions)
                  .build()))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramBlockDefinitionNotFoundException
        | ProgramNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /** HTMX form that re-renders a predicate condition, dropping a subcondition from the DOM. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxDeleteSubcondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    DynamicForm form = formFactory.form().bindFromRequest(request);
    if (form.hasErrors()
        || form.rawData().get("conditionId") == null
        || form.rawData().get("subconditionId") == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
    Long conditionId = Long.valueOf(form.rawData().get("conditionId"));
    Long subconditionId = Long.valueOf(form.rawData().get("subconditionId"));

    try {
      Map<String, String> formData = new HashMap<>(form.rawData());

      // Pre-filter formData to only include fields for the condition we're editing.
      // Also exclude fields for the deleted subcondition.
      String parentCondition = String.format("condition-%d", conditionId);
      String removedSubconditionPrefix =
          String.format("condition-%d-subcondition-%d", conditionId, subconditionId);
      formData
          .keySet()
          .removeIf(
              key -> !key.startsWith(parentCondition) || key.startsWith(removedSubconditionPrefix));

      EditConditionPartialViewModel condition =
          getOnlyElement(
              buildConditionsListFromFormData(
                  programId, blockDefinitionId, predicateUseCase, ImmutableMap.copyOf(formData)));
      ImmutableList<EditSubconditionPartialViewModel> subconditions =
          condition.subconditions().isEmpty()
              ? ImmutableList.of(condition.emptySubconditionViewModel())
              : condition.subconditions();

      return ok(subconditionListPartialView.render(
              request,
              SubconditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .conditionId(conditionId)
                  .subconditions(subconditions)
                  .build()))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramBlockDefinitionNotFoundException
        | ProgramNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
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
        // This should never happen since we filter out Enumerator questions before this
        // point.
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
    ImmutableList<QuestionType> multiSelectQuestionTypes =
        ImmutableList.of(
            QuestionType.DROPDOWN,
            QuestionType.CHECKBOX,
            QuestionType.RADIO_BUTTON,
            QuestionType.YES_NO);
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
    } else if (multiSelectQuestionTypes.contains(question.getQuestionType())) {
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) question;
      return multiOptionQuestionDefinition.getDisplayableOptions().stream()
          .map(
              option ->
                  OptionElement.builder()
                      .value(option.adminName())
                      .displayText(option.optionText().getOrDefault(Locale.US))
                      .selected(false)
                      .build())
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

  /**
   * Given formData from a dynamic form, returns a parsed list of {@link
   * EditConditionPartialViewModel} iterates through top-level conditions and subconditions in order
   * of condition / subconditionId.
   *
   * <p>Does nothing if no condition / subcondition ID fields are present. Skips conditions with no
   * subconditions.
   */
  private ImmutableList<EditConditionPartialViewModel> buildConditionsListFromFormData(
      Long programId,
      Long blockDefinitionId,
      String predicateUseCase,
      ImmutableMap<String, String> formData)
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    ArrayList<EditConditionPartialViewModel> editConditionModels = new ArrayList<>();
    PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
    ImmutableList<QuestionDefinition> availableQuestions =
        getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, useCase);
    ImmutableList<OptionElement> operatorOptions = getOperatorOptions();
    ImmutableList<OptionElement> defaultQuestionOptions =
        getQuestionOptions(availableQuestions, /* selectedQuestion= */ Optional.empty());

    // Get list of present condition IDs.
    // This is necessary to account for gaps in condition IDs.
    Pattern conditionIdPattern = Pattern.compile("^condition-(\\d+)");
    ImmutableList<Long> presentConditionIds =
        ImmutableList.sortedCopyOf(
            formData.keySet().stream()
                .map(conditionIdPattern::matcher)
                .filter(Matcher::find)
                .map(match -> Long.parseLong(match.group(1)))
                .collect(ImmutableSet.toImmutableSet()));

    // Iterate upwards through condition IDs.
    for (Long conditionId : presentConditionIds) {
      EditConditionPartialViewModel condition =
          EditConditionPartialViewModel.builder()
              .programId(programId)
              .blockId(blockDefinitionId)
              .predicateUseCase(useCase)
              .questionOptions(defaultQuestionOptions)
              .operatorOptions(operatorOptions)
              .build();

      /* Iterate through subconditions */
      ArrayList<EditSubconditionPartialViewModel> subconditions = new ArrayList<>();
      Pattern subconditionIdPattern =
          Pattern.compile(String.format("^condition-%d-subcondition-(\\d+)", conditionId));
      ImmutableList<Long> presentSubconditionIds =
          ImmutableList.sortedCopyOf(
              formData.keySet().stream()
                  .map(subconditionIdPattern::matcher)
                  .filter(Matcher::find)
                  .map(match -> Long.parseLong(match.group(1)))
                  .collect(ImmutableSet.toImmutableSet()));

      /// Keep going until we run out of user-entered subconditions.
      for (int i = 0; i < presentSubconditionIds.size(); i++) {
        Long subconditionId = presentSubconditionIds.get(i);
        String subconditionPrefix =
            String.format("condition-%d-subcondition-%d", conditionId, subconditionId);

        EditSubconditionPartialViewModelBuilder subconditionBuilder =
            condition.emptySubconditionViewModel().toBuilder();

        // Set the user-selected question
        // If there isn't a question key present, something is misformatted and we should skip.
        String questionFieldName = subconditionPrefix + "-question";
        String questionFieldValue = formData.get(questionFieldName);
        if (!StringUtils.isNumeric(questionFieldValue)
            || !formData.containsKey(questionFieldName)) {
          subconditions.add(subconditionBuilder.build());
          continue;
        }

        Long questionId = Long.valueOf(questionFieldValue);
        Optional<QuestionDefinition> selectedQuestion =
            availableQuestions.stream()
                .filter(question -> questionId.equals(question.getId()))
                .findFirst();

        // From selectedQuestion, we can get the rest of the necessary fields.
        // For the last subcondition, we should render the "Add subcondition" button.
        subconditionBuilder
            .selectedQuestionType(
                selectedQuestion.map(question -> question.getQuestionType().getLabel()))
            .questionOptions(getQuestionOptions(availableQuestions, selectedQuestion))
            .scalarOptions(
                selectedQuestion
                    .map(question -> getScalarOptionsForQuestion(question))
                    .orElse(ImmutableList.of()))
            .operatorOptions(operatorOptions)
            .valueOptions(
                selectedQuestion
                    .map(question -> getValueOptionsForQuestion(question))
                    .orElse(ImmutableList.of()));

        subconditions.add(subconditionBuilder.build());
      }

      condition = condition.toBuilder().subconditions(ImmutableList.copyOf(subconditions)).build();
      editConditionModels.add(condition);
    }

    return ImmutableList.copyOf(editConditionModels);
  }
}
