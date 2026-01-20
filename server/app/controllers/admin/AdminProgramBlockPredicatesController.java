package controllers.admin;

import static com.google.common.base.Enums.getIfPresent;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import services.geo.esri.EsriServiceAreaValidationOption;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.EligibilityNotValidForProgramTypeException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateExpressionNodeType;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateLogicalOperator;
import services.program.predicate.PredicateUseCase;
import services.program.predicate.SelectedValue;
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
import views.html.helper.form;

/**
 * Controller for admins editing and viewing program predicates for eligibility and visibility
 * logic.
 */
public class AdminProgramBlockPredicatesController extends CiviFormController {
  // List of operator types for which we expect two input fields
  private static final ImmutableList<Operator> INPUT_PAIR_OPERATOR_TYPES =
      ImmutableList.of(Operator.BETWEEN, Operator.AGE_BETWEEN);

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
        Optional<PredicateDefinition> maybePredicateDefinition =
            getAvailablePredicateDefinition(programDefinition, blockDefinitionId, predicateUseCase);

        ImmutableList<EditConditionPartialViewModel> populatedConditionsList =
            buildConditionsListFromPredicateDefinition(
                programId, blockDefinitionId, predicateUseCase, maybePredicateDefinition);

        EditPredicatePageViewModel model =
            EditPredicatePageViewModel.builder()
                .programDefinition(programDefinition)
                .blockDefinition(blockDefinition)
                .predicateUseCase(predicateUseCase)
                .operatorScalarMap(getOperatorScalarMap())
                .prePopulatedConditions(populatedConditionsList)
                .hasAvailableQuestions(!predicateQuestions.isEmpty())
                .rootLogicalOperator(getRootLogicalOperator(maybePredicateDefinition))
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
    } catch (QuestionNotFoundException e) {
      return notFound(e.getLocalizedMessage());
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

  private Optional<PredicateDefinition> getAvailablePredicateDefinition(
      ProgramDefinition programDefinition,
      long blockDefinitionId,
      PredicateUseCase predicateUseCase)
      throws ProgramBlockDefinitionNotFoundException {
    BlockDefinition blockDefinition =
        programDefinition.blockDefinitions().stream()
            .filter(block -> block.id() == blockDefinitionId)
            .findFirst()
            .orElseThrow(
                () ->
                    new ProgramBlockDefinitionNotFoundException(
                        programDefinition.id(), blockDefinitionId));

    return switch (predicateUseCase) {
      case ELIGIBILITY ->
          blockDefinition.eligibilityDefinition().map(EligibilityDefinition::predicate);
      case VISIBILITY -> blockDefinition.visibilityPredicate();
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

  // Gets the {@link PredicateLogicalOperator} of the root node, defaulting to "AND".
  private PredicateLogicalOperator getRootLogicalOperator(
      Optional<PredicateDefinition> maybePredicateDefinition) {
    return maybePredicateDefinition
        .map(
            predicateDefinition ->
                predicateDefinition.rootNode().getType().equals(PredicateExpressionNodeType.OR)
                    ? PredicateLogicalOperator.OR
                    : PredicateLogicalOperator.AND)
        .orElse(PredicateLogicalOperator.AND);
  }

  // Builds a subcondition model from a LeafOperationExpressionNode.
  private EditSubconditionPartialViewModel buildSubconditionFromLeafNode(
      EditSubconditionPartialViewModel model,
      LeafOperationExpressionNode leafNode,
      ImmutableList<QuestionDefinition> availableQuestions)
      throws QuestionNotFoundException {
    QuestionDefinition selectedQuestion =
        availableQuestions.stream()
            .filter(q -> q.getId() == leafNode.questionId())
            .findFirst()
            .orElseThrow(() -> new QuestionNotFoundException(leafNode.questionId()));

    SelectedValue userEnteredValue =
        leafNode.comparedValue().toSelectedValue(selectedQuestion.getQuestionType());

    // Grab user-entered text to populate text fields.
    // For cases where we expect multi-value inputs (like checkbox questions),
    // we use "valueOptions" below and the text fields aren't shown.
    Optional<String> firstValueOptional =
        switch (userEnteredValue.getKind()) {
          case SINGLE -> Optional.of(userEnteredValue.single());
          case PAIR -> Optional.of(userEnteredValue.pair().first());
          case MULTIPLE -> Optional.empty();
        };

    Optional<String> secondValueOptional =
        switch (userEnteredValue.getKind()) {
          case PAIR -> Optional.of(userEnteredValue.pair().second());
          case SINGLE, MULTIPLE -> Optional.empty();
        };

    return model.toBuilder()
        .questionOptions(getQuestionOptions(availableQuestions, Optional.of(selectedQuestion)))
        .operatorOptions(getOperatorOptions(Optional.of(leafNode.operator())))
        .scalarOptions(
            getScalarOptionsForQuestion(selectedQuestion, Optional.of(leafNode.scalar())))
        .selectedQuestionType(Optional.of(selectedQuestion.getQuestionType().getLabel()))
        .userEnteredValue(firstValueOptional.orElse(""))
        .secondUserEnteredValue(secondValueOptional.orElse(""))
        .valueOptions(getValueOptionsForQuestion(selectedQuestion, userEnteredValue))
        .build();
  }

  // Builds a subcondition model from a LeafAddressServiceAreaExpressionNode.
  private EditSubconditionPartialViewModel buildSubconditionFromAddressNode(
      EditSubconditionPartialViewModel model,
      LeafAddressServiceAreaExpressionNode addressNode,
      ImmutableList<QuestionDefinition> availableQuestions)
      throws QuestionNotFoundException {
    QuestionDefinition selectedQuestion =
        availableQuestions.stream()
            .filter(q -> q.getId() == addressNode.questionId())
            .findFirst()
            .orElseThrow(() -> new QuestionNotFoundException(addressNode.questionId()));
    SelectedValue userEnteredValue = SelectedValue.single(addressNode.serviceAreaId());

    return model.toBuilder()
        .questionOptions(getQuestionOptions(availableQuestions, Optional.of(selectedQuestion)))
        .operatorOptions(getOperatorOptions(Optional.of(addressNode.operator())))
        .scalarOptions(
            getScalarOptionsForQuestion(selectedQuestion, /* selectedScalar= */ Optional.empty()))
        .selectedQuestionType(Optional.of(selectedQuestion.getQuestionType().getLabel()))
        .valueOptions(getValueOptionsForQuestion(selectedQuestion, userEnteredValue))
        .build();
  }

  /**
   * Builds a list of condition view models from an saved {@link PredicateDefinition}.
   *
   * <p>If expanded form logic is not enabled, or there is no saved predicate definition, returns an
   * empty list.
   */
  private ImmutableList<EditConditionPartialViewModel> buildConditionsListFromPredicateDefinition(
      long programId,
      long blockDefinitionId,
      PredicateUseCase predicateUseCase,
      Optional<PredicateDefinition> maybePredicateDefinition)
      throws QuestionNotFoundException,
          ProgramBlockDefinitionNotFoundException,
          ProgramNotFoundException {
    if (maybePredicateDefinition.isEmpty()) {
      return ImmutableList.of();
    }
    requestChecker.throwIfProgramNotDraft(programId);

    // If the root is a leaf node, treat it as a single condition.
    // If it's an AND/OR node, its children are the conditions.
    ImmutableList<PredicateExpressionNode> conditionExpressionNodes =
        getLeafNodeOrChildren(maybePredicateDefinition.get().rootNode());

    ImmutableList<QuestionDefinition> availableQuestions =
        getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, predicateUseCase);

    // Iterate through top-level condition nodes (children of root AND/OR) to build view models.
    // Nested for-loop: iterate through each top-level condition and all of their subconditions.
    ArrayList<EditConditionPartialViewModel> conditionsList = new ArrayList<>();
    for (PredicateExpressionNode conditionExpressionNode : conditionExpressionNodes) {
      PredicateLogicalOperator subconditionLogicalOperator = PredicateLogicalOperator.AND;
      if (!conditionExpressionNode.getType().isLeafNode()) {
        subconditionLogicalOperator =
            conditionExpressionNode.getType().equals(PredicateExpressionNodeType.AND)
                ? PredicateLogicalOperator.AND
                : PredicateLogicalOperator.OR;
      }
      EditConditionPartialViewModel conditionViewModel =
          createBaseConditionModel(
              programId,
              blockDefinitionId,
              predicateUseCase,
              availableQuestions,
              subconditionLogicalOperator);

      conditionViewModel =
          conditionViewModel.toBuilder()
              .subconditions(
                  buildSubconditionsListFromDefinedCondition(
                      conditionExpressionNode,
                      conditionViewModel.emptySubconditionViewModel(),
                      availableQuestions))
              .build();

      conditionsList.add(conditionViewModel);
    }

    return ImmutableList.copyOf(conditionsList);
  }

  /**
   * Given a single {@link PredicateExpressionNode} mapping to a predicate condition, build a list
   * of that condition's subcondition nodes.
   */
  private ImmutableList<EditSubconditionPartialViewModel>
      buildSubconditionsListFromDefinedCondition(
          PredicateExpressionNode conditionExpressionNode,
          EditSubconditionPartialViewModel baseSubconditionModel,
          ImmutableList<QuestionDefinition> availableQuestions)
          throws QuestionNotFoundException {
    // If the conditionExpressionNode is a leaf node, treat it as a single subcondition.
    ArrayList<EditSubconditionPartialViewModel> subconditionsList = new ArrayList<>();
    ImmutableList<PredicateExpressionNode> subconditionExpressionNodes =
        getLeafNodeOrChildren(conditionExpressionNode);

    for (PredicateExpressionNode subconditionExpressionNode : subconditionExpressionNodes) {
      EditSubconditionPartialViewModel subconditionViewModel = baseSubconditionModel;

      switch (subconditionExpressionNode.getType()) {
        case LEAF_OPERATION -> {
          subconditionViewModel =
              buildSubconditionFromLeafNode(
                  subconditionViewModel,
                  subconditionExpressionNode.getLeafOperationNode(),
                  availableQuestions);
        }
        case LEAF_ADDRESS_SERVICE_AREA -> {
          subconditionViewModel =
              buildSubconditionFromAddressNode(
                  subconditionViewModel,
                  subconditionExpressionNode.getLeafAddressNode(),
                  availableQuestions);
        }
        case AND, OR -> {
          // Skip non-leaf nodes.
        }
      }
      subconditionsList.add(subconditionViewModel);
    }

    return ImmutableList.copyOf(subconditionsList);
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

      // Validate input fields
      ImmutableList<EditConditionPartialViewModel> validatedConditionsList =
          buildConditionsListFromFormData(
              programId,
              blockDefinitionId,
              predicateUseCase,
              ImmutableMap.copyOf(form.rawData()),
              /* validateInputFields= */ true);

      // If there are invalid input fields present, return to the same screen.
      if (predicateHasInvalidInputs(validatedConditionsList)) {
        return ok(conditionListPartialView.render(
                request,
                ConditionListPartialViewModel.builder()
                    .programId(programId)
                    .blockId(blockDefinitionId)
                    .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                    .conditions(validatedConditionsList)
                    .build()))
            .as(Http.MimeTypes.HTML);
      }

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
      return ok().withHeader(
              "HX-Redirect",
              routes.AdminProgramBlocksController.edit(programId, blockDefinitionId).url())
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    // TODO(#11761): Replace toast with dismissable alert when admin alerts are
    // ready.
    return ok().withHeader(
            "HX-Redirect",
            routes.AdminProgramBlocksController.edit(programId, blockDefinitionId).url())
        .flashing(
            FlashKey.SUCCESS,
            String.format("Saved %s condition", predicateUseCase.toLowerCase(Locale.ROOT)));
  }

  /** HTMX partial that renders a card for adding a condition to a predicate. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxAddCondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    DynamicForm form = formFactory.form().bindFromRequest(request);
    ImmutableMap<String, String> formData = ImmutableMap.copyOf(form.rawData());
    if (formData.get("conditionId") == null) {
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
                  formData,
                  /* validateInputFields= */ false));
      EditConditionPartialViewModel condition =
          EditConditionPartialViewModel.builder()
              .programId(programId)
              .blockId(blockDefinitionId)
              .predicateUseCase(useCase)
              .questionOptions(
                  getQuestionOptions(availableQuestions, /* selectedQuestion= */ Optional.empty()))
              .subconditionLogicalOperator(PredicateLogicalOperator.AND)
              .scalarOptions(ImmutableList.of())
              .operatorOptions(getOperatorOptions(/* selectedOperator= */ Optional.empty()))
              .build();

      // If there are existing conditions, autofocus the new condition's logic dropdown.
      // Otherwise, focus the root logic dropdown.
      boolean isExistingPredicate = !currentConditions.isEmpty();
      condition =
          condition.toBuilder()
              .subconditions(
                  ImmutableList.of(
                      condition.emptySubconditionViewModel().toBuilder()
                          .autofocus(false)
                          .shouldAnnounceChanges(false)
                          .build()))
              .focusLogicDropdown(isExistingPredicate)
              .build();
      currentConditions.add(condition);

      // Render an updated list of conditions (sorted by conditionId).
      return ok(conditionListPartialView.render(
              request,
              ConditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(useCase)
                  .predicateLogicalOperator(getLogicalOperatorFromFormData("root", formData))
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
   * <p>Used to update an existing subcondition form when a question is changed.
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
      Long conditionId = Long.valueOf(formData.get("conditionId"));
      Long subconditionId = Long.valueOf(form.get("subconditionId"));

      // Dynamic forms contain full form from the request.
      // We need to start by filtering to only this condition.
      String parentCondition = String.format("condition-%d", conditionId);
      formData.keySet().removeIf(key -> !key.startsWith(parentCondition));

      // The built condition list will contain any current edits.
      EditConditionPartialViewModel condition =
          getOnlyElement(
              buildConditionsListFromFormData(
                  programId,
                  blockDefinitionId,
                  predicateUseCase,
                  ImmutableMap.copyOf(formData),
                  /* validateInputFields= */ false));

      // Focus only the edited subcondition
      int focusedIndex = subconditionId.intValue() - 1;
      ImmutableList<EditSubconditionPartialViewModel> subconditionList =
          getOnlyElement(
                  focusSubconditionInList(
                      ImmutableList.of(condition),
                      /* conditionIndex= */ 0,
                      focusedIndex,
                      /* shouldAnnounceChanges= */ false))
              .subconditions();

      return ok(subconditionListPartialView.render(
              request,
              SubconditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .predicateLogicalOperator(condition.subconditionLogicalOperator())
                  .conditionId(conditionId)
                  .subconditions(subconditionList)
                  .build()))
          .as(Http.MimeTypes.HTML);
    } catch (ProgramNotFoundException
        | ProgramBlockDefinitionNotFoundException
        | IllegalArgumentException e) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
  }

  /** HTMX endpoint that adds a new subcondition underneath a predicate condition. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxAddSubcondition(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }
    DynamicForm form = formFactory.form().bindFromRequest(request);
    if (form.hasErrors() || form.get("conditionId") == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }

    try {
      Map<String, String> formData = new HashMap<>(form.rawData());
      long conditionId = Long.valueOf(formData.get("conditionId"));

      // Dynamic forms contain full form from the request.
      // We need to start by filtering to only this condition.
      String parentCondition = String.format("condition-%d", conditionId);
      formData.keySet().removeIf(key -> !key.startsWith(parentCondition));

      // If the user is editing a subcondition, the built condition list will contain those edits.
      EditConditionPartialViewModel condition =
          getOnlyElement(
              buildConditionsListFromFormData(
                  programId,
                  blockDefinitionId,
                  predicateUseCase,
                  ImmutableMap.copyOf(formData),
                  /* validateInputFields= */ false));
      ArrayList<EditSubconditionPartialViewModel> subconditionList =
          new ArrayList<>(condition.subconditions());

      subconditionList.add(
          condition.emptySubconditionViewModel().toBuilder()
              .autofocus(true)
              .shouldAnnounceChanges(true)
              .build());

      return ok(subconditionListPartialView.render(
              request,
              SubconditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .predicateLogicalOperator(condition.subconditionLogicalOperator())
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
    if (form.hasErrors() || form.get("conditionId") == null) {
      return ok(failedRequestPartialView.render(request, new FailedRequestPartialViewModel()))
          .as(Http.MimeTypes.HTML);
    }
    int conditionId = Integer.parseInt(form.rawData().get("conditionId"));

    try {
      String removedConditionPrefix = "condition-" + conditionId;
      Map<String, String> formData = new HashMap<>(form.rawData());

      // Start by pre-filtering formData to remove entry for the deleted condition.
      formData.keySet().removeIf(key -> key.startsWith(removedConditionPrefix));

      ArrayList<EditConditionPartialViewModel> conditions =
          new ArrayList<>(
              buildConditionsListFromFormData(
                  programId,
                  blockDefinitionId,
                  predicateUseCase,
                  ImmutableMap.copyOf(formData),
                  /* validateInputFields= */ false));

      // Handle accessibility steps (skip if there are no conditions left).
      // Focus either: subcondition 1 of the previous condition OR subcondition 1 of condition 1.
      // (Note: conditionId is 1-indexed, lists are 0-indexed.)
      if (!conditions.isEmpty()) {
        int focusedConditionIndex = Integer.max(0, conditionId - 2);
        EditConditionPartialViewModel focusedCondition = conditions.get(focusedConditionIndex);
        focusedCondition = focusedCondition.toBuilder().focusLogicDropdown(true).build();
        conditions.set(focusedConditionIndex, focusedCondition);
      }

      return ok(conditionListPartialView.render(
              request,
              ConditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .predicateLogicalOperator(
                      getLogicalOperatorFromFormData("root", ImmutableMap.copyOf(formData)))
                  .conditions(ImmutableList.copyOf(conditions))
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
                  programId,
                  blockDefinitionId,
                  predicateUseCase,
                  ImmutableMap.copyOf(formData),
                  /* validateInputFields= */ false));

      // Focus either the previous (zero-indexed) subcondition index before the deleted
      // subcondition, or the first subcondition.
      // All other subconditions should be unfocused.
      int autofocusedSubcondition = Integer.max(0, subconditionId.intValue() - 2);
      ImmutableList<EditSubconditionPartialViewModel> subconditions =
          condition.subconditions().isEmpty()
              ? ImmutableList.of(
                  condition.emptySubconditionViewModel().toBuilder()
                      .autofocus(true)
                      .shouldAnnounceChanges(false)
                      .build())
              : getOnlyElement(
                      focusSubconditionInList(
                          ImmutableList.of(condition),
                          /* conditionIndex= */ 0,
                          autofocusedSubcondition,
                          /* shouldAnnounceChanges= */ true))
                  .subconditions();

      return ok(subconditionListPartialView.render(
              request,
              SubconditionListPartialViewModel.builder()
                  .programId(programId)
                  .blockId(blockDefinitionId)
                  .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                  .predicateLogicalOperator(condition.subconditionLogicalOperator())
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

  /** HTMX form that re-renders an empty conditions list. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxDeleteAllConditions(
      Request request, long programId, long blockDefinitionId, String predicateUseCase) {
    if (!settingsManifest.getExpandedFormLogicEnabled(request)) {
      return notFound("Expanded form logic is not enabled.");
    }

    return ok(conditionListPartialView.render(
            request,
            ConditionListPartialViewModel.builder()
                .programId(programId)
                .blockId(blockDefinitionId)
                .predicateUseCase(PredicateUseCase.valueOf(predicateUseCase))
                .conditions(ImmutableList.of())
                .build()))
        .as(Http.MimeTypes.HTML);
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
      QuestionDefinition question, Optional<Scalar> selectedScalar) {
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
    boolean validScalarSelection =
        selectedScalar.isPresent() && scalars.contains(selectedScalar.get());
    AtomicBoolean isFirst = new AtomicBoolean(true);
    scalars.forEach(
        scalar -> {
          // Select either the user-selected scalar if selectedScalar is valid OR the first in the
          // list if selectedScalar is invalid.
          boolean shouldSelect =
              validScalarSelection
                  ? scalar.name().equals(selectedScalar.get().name())
                  : isFirst.getAndSet(false);
          scalarOptionsBuilder.add(
              ScalarOptionElement.builder()
                  .value(scalar.name())
                  .displayText(scalar.toDisplayString())
                  .scalarType(scalar.toScalarType().name())
                  .selected(shouldSelect)
                  .build());
        });

    return scalarOptionsBuilder.build();
  }

  /**
   * Returns a list of {@link OptionElement}s representing all possible Values for the condition,
   * depending on question type.
   */
  private ImmutableList<OptionElement> getValueOptionsForQuestion(
      QuestionDefinition question, SelectedValue selectedValue) {
    AtomicBoolean isFirst = new AtomicBoolean(true);
    if (question.isAddress()) {
      checkState(selectedValue.getKind().equals(SelectedValue.Kind.SINGLE));
      // Check whether the selected value is a valid address.
      ImmutableMap<String, EsriServiceAreaValidationOption> addressOptions =
          esriServiceAreaValidationConfig.getImmutableMap();
      boolean valueIsAddress =
          addressOptions.entrySet().stream()
              .anyMatch(entry -> entry.getValue().getLabel().equals(selectedValue.single()));
      return addressOptions.entrySet().stream()
          .map(
              entry -> {
                String displayText = entry.getValue().getLabel();
                // Select either the already-selected address value OR the first in the list of
                // options if no address is selected.
                boolean shouldSelect =
                    valueIsAddress
                        ? displayText.equals(selectedValue.single())
                        : isFirst.getAndSet(false);
                return OptionElement.builder()
                    .value(entry.getKey())
                    .displayText(displayText)
                    .selected(shouldSelect)
                    .build();
              })
          .collect(ImmutableList.toImmutableList());
    } else if (question.getQuestionType().isMultiOptionType()) {
      checkState(selectedValue.getKind().equals(SelectedValue.Kind.MULTIPLE));
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) question;
      return multiOptionQuestionDefinition.getDisplayableOptions().stream()
          .map(
              option -> {
                String value = String.valueOf(option.id());
                boolean shouldSelect = selectedValue.multiple().contains(value);
                return OptionElement.builder()
                    .value(value)
                    .displayText(option.optionText().getOrDefault(Locale.US))
                    .selected(shouldSelect)
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
  private ImmutableList<OptionElement> getOperatorOptions(Optional<Operator> selectedOperator) {
    return Arrays.stream(Operator.values())
        .map(
            operator -> {
              boolean shouldSelect =
                  selectedOperator
                      .map(Operator::name)
                      .map(name -> name.equals(operator.name()))
                      .orElse(false);
              return OptionElement.builder()
                  .value(operator.name())
                  .displayText(operator.toDisplayString())
                  .selected(shouldSelect)
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
      ImmutableMap<String, String> formData,
      boolean validateInputFields)
      throws ProgramBlockDefinitionNotFoundException, ProgramNotFoundException {
    ArrayList<EditConditionPartialViewModel> editConditionModels = new ArrayList<>();
    PredicateUseCase useCase = PredicateUseCase.valueOf(predicateUseCase);
    ImmutableList<QuestionDefinition> availableQuestions =
        getAvailablePredicateQuestionDefinitions(programId, blockDefinitionId, useCase);

    // Get list of present condition IDs.
    // This is necessary to account for gaps in condition IDs.
    Pattern conditionIdPattern = Pattern.compile("^condition-(\\d+)");
    ImmutableList<Long> presentConditionIds =
        getSortedMatchesFromKeys(conditionIdPattern, formData);

    // Iterate upwards through condition IDs.
    for (Long conditionId : presentConditionIds) {
      PredicateLogicalOperator subconditionLogicalOperator =
          getLogicalOperatorFromFormData(String.format("condition-%d", conditionId), formData);
      EditConditionPartialViewModel condition =
          createBaseConditionModel(
              programId,
              blockDefinitionId,
              useCase,
              availableQuestions,
              subconditionLogicalOperator);

      /* Iterate through subconditions */
      ArrayList<EditSubconditionPartialViewModel> subconditions = new ArrayList<>();
      Pattern subconditionIdPattern =
          Pattern.compile(String.format("^condition-%d-subcondition-(\\d+)", conditionId));
      ImmutableList<Long> presentSubconditionIds =
          getSortedMatchesFromKeys(subconditionIdPattern, formData);

      /// Keep going until we run out of user-entered subconditions.
      for (long subconditionId : presentSubconditionIds) {
        String subconditionFieldPrefix =
            String.format("condition-%d-subcondition-%d", conditionId, subconditionId);
        subconditions.add(
            getParsedSubconditionFromFormData(
                condition.emptySubconditionViewModel(),
                subconditionFieldPrefix,
                availableQuestions,
                formData,
                validateInputFields));
      }

      condition = condition.toBuilder().subconditions(ImmutableList.copyOf(subconditions)).build();
      editConditionModels.add(condition);
    }

    return ImmutableList.copyOf(editConditionModels);
  }

  /**
   * Given formData from a dynamic form and an empty builder, return a single parsed subcondition,
   * if present.
   *
   * <p>Expected keys:
   *
   * <ul>
   *   <li>Operator: "{fieldNamePrefix}-operator"
   *   <li>Scalar: "{fieldNamePrefix}-scalar"
   *   <li>Value: "{fieldNamePrefix}-value"
   *   <li>Second value (for BETWEEN operator): "{fieldNamePrefix}-secondValue"
   * </ul>
   *
   * @param emptyModel An empty builder for an EditSubconditionPartialViewModel. Will be used to
   *     build the returned subcondition.
   * @param fieldNamePrefix A prefix for subcondition field names. Expected to be of format
   *     "condition-{conditionId}-subcondition-{subconditionId}".
   * @param availableQuestions All questions available in this program.
   * @param formData The dynamic form data, containing user-entered values for this subcondition.
   * @param validateInputFields Whether or not to validate the presence of form inputs. Validation
   *     data is stored in the returned EditSubconditionPartialViewModel.
   */
  private EditSubconditionPartialViewModel getParsedSubconditionFromFormData(
      EditSubconditionPartialViewModel emptyModel,
      String fieldNamePrefix,
      ImmutableList<QuestionDefinition> availableQuestions,
      ImmutableMap<String, String> formData,
      boolean validateInputFields) {
    EditSubconditionPartialViewModelBuilder subconditionBuilder = emptyModel.toBuilder();

    // Set the user-selected question
    // If there isn't a question key present, something is misformatted and we should return
    // immediately.
    String questionFieldName = fieldNamePrefix + "-question";
    String questionFieldValue = formData.get(questionFieldName);
    if (!StringUtils.isNumeric(questionFieldValue) || !formData.containsKey(questionFieldName)) {
      return subconditionBuilder.build();
    }

    Long questionId = Long.valueOf(questionFieldValue);
    Optional<QuestionDefinition> selectedQuestion =
        availableQuestions.stream()
            .filter(question -> questionId.equals(question.getId()))
            .findFirst();

    // (Optionally) set the user-selected operator and scalar.
    Optional<Operator> selectedOperatorOptional =
        optionallyGetEnumFromFormData(Operator.class, formData, fieldNamePrefix + "-operator");
    Optional<Scalar> selectedScalarOptional =
        optionallyGetEnumFromFormData(Scalar.class, formData, fieldNamePrefix + "-scalar");

    // Get the user-entered values, if present. Empty string otherwise.
    String inputFieldId = fieldNamePrefix + "-value";
    String secondInputFieldId = fieldNamePrefix + "-secondValue";

    String inputFieldValue = Objects.toString(formData.get(inputFieldId), "");
    String secondInputFieldValue =
        Objects.toString(formData.get(fieldNamePrefix + "-secondValue"), "");

    // Get multiValue selections (radios, dropdowns, checkboxes, etc.), if present.
    ImmutableSet<String> multiValueSelections =
        getMultiValueSelectionsFromFormData(fieldNamePrefix, formData);

    // Populate the selected value, depending on whether this is a multi-value question type.
    SelectedValue selectedValue =
        selectedQuestion
                .map(question -> question.getQuestionType().isMultiOptionType())
                .orElse(false)
            ? SelectedValue.multiple(multiValueSelections)
            : SelectedValue.single(inputFieldValue);

    // Perform input validation
    ArrayList<String> invalidFieldIds = new ArrayList<>();
    if (validateInputFields) {
      // First input field
      // Ignore cases where the user selected a multi-value question.
      if (selectedQuestion.isPresent()
          && !selectedQuestion.get().getQuestionType().isMultiOptionType()
          && inputFieldValue.isBlank()) {
        invalidFieldIds.add(inputFieldId);
      }
      // Second input field
      // Ignore cases where we're not expecting a pair of inputs.
      if (selectedOperatorOptional.isPresent()
          && INPUT_PAIR_OPERATOR_TYPES.contains(selectedOperatorOptional.get())
          && secondInputFieldValue.isBlank()) {
        invalidFieldIds.add(secondInputFieldId);
      }
      // Multi-value checkboxes
      // If there's nothing entered in multi-value selections, mark the whole field as invalid.
      // If any value is present in invalidInputIds, we invalidate the multivalue question.
      if (multiValueSelections.isEmpty()
          && selectedValue.getKind().equals(SelectedValue.Kind.MULTIPLE)) {
        invalidFieldIds.add(fieldNamePrefix);
      }
    }

    return subconditionBuilder
        .selectedQuestionType(
            selectedQuestion.map(question -> question.getQuestionType().getLabel()))
        .questionOptions(getQuestionOptions(availableQuestions, selectedQuestion))
        .scalarOptions(
            selectedQuestion.isPresent()
                ? getScalarOptionsForQuestion(selectedQuestion.get(), selectedScalarOptional)
                : ImmutableList.of())
        .operatorOptions(getOperatorOptions(selectedOperatorOptional))
        .valueOptions(
            selectedQuestion.isPresent()
                ? getValueOptionsForQuestion(selectedQuestion.get(), selectedValue)
                : ImmutableList.of())
        .invalidInputIds(ImmutableList.copyOf(invalidFieldIds))
        .userEnteredValue(inputFieldValue)
        .secondUserEnteredValue(secondInputFieldValue)
        .build();
  }

  /**
   * Given form data from a dynamic form, find the logical operator under the node specified by
   * fieldNamePrefix.
   */
  private PredicateLogicalOperator getLogicalOperatorFromFormData(
      String fieldNamePrefix, ImmutableMap<String, String> formData) {
    String nodeTypeId = fieldNamePrefix + "-node-type";
    // NodeType should always be present in the form data.
    checkState(formData.containsKey(nodeTypeId));

    String logicalOperatorString = formData.get(nodeTypeId);

    return PredicateLogicalOperator.valueOf(logicalOperatorString);
  }

  /**
   * Given formData from a dynamic form, find and return set of multi-value selections for the given
   * subcondition, if any are present.
   *
   * @param fieldNamePrefix A prefix for the expected value field names, containing the condition
   *     and subcondition IDs. Expected to be of format
   *     "condition-{conditionId}-subcondition-{subconditionId}-values[{valueNum}]".
   * @param formData The dynamic form data included in the top-level HTMX request. Contains
   *     user-entered values.
   */
  private ImmutableSet<String> getMultiValueSelectionsFromFormData(
      String fieldNamePrefix, ImmutableMap<String, String> formData) {
    // Find present input fields and return set of the IDs.
    Pattern valuesIdPattern =
        Pattern.compile(String.format("^%s-values\\[(\\d+)\\]", fieldNamePrefix));
    ImmutableList<Long> presentValueInputNums = getSortedMatchesFromKeys(valuesIdPattern, formData);

    Set<String> valuesToReturn = new HashSet<>();

    // Iterate through present input fields, collect all values.
    for (Long inputNum : presentValueInputNums) {
      String inputFieldName = String.format("%s-values[%d]", fieldNamePrefix, inputNum);

      if (formData.containsKey(inputFieldName)) {
        valuesToReturn.add(formData.get(inputFieldName));
      }
    }

    return ImmutableSet.copyOf(valuesToReturn);
  }

  // Create a base EditConditionPartialViewModel with default options.
  private EditConditionPartialViewModel createBaseConditionModel(
      long programId,
      long blockDefinitionId,
      PredicateUseCase predicateUseCase,
      ImmutableList<QuestionDefinition> availableQuestions,
      PredicateLogicalOperator subconditionLogicalOperator) {
    return EditConditionPartialViewModel.builder()
        .programId(programId)
        .blockId(blockDefinitionId)
        .predicateUseCase(predicateUseCase)
        .questionOptions(
            getQuestionOptions(availableQuestions, /* selectedQuestion= */ Optional.empty()))
        .scalarOptions(ImmutableList.of())
        .operatorOptions(getOperatorOptions(/* selectedOperator= */ Optional.empty()))
        .subconditionLogicalOperator(subconditionLogicalOperator)
        .focusLogicDropdown(false)
        .build();
  }

  // If the given node is a leaf node, return a list containing just that node. Otherwise, return
  // its children.
  private ImmutableList<PredicateExpressionNode> getLeafNodeOrChildren(
      PredicateExpressionNode node) {
    if (node.getType().isLeafNode()) {
      return ImmutableList.of(node);
    } else {
      return node.getChildren();
    }
  }

  // Given a regex pattern keyPattern and a map, return a sorted list of matches between
  // map.keySet() and keyPattern, selecting the first group from the regex.
  private ImmutableList<Long> getSortedMatchesFromKeys(
      Pattern keyPattern, ImmutableMap<String, String> map) {
    return ImmutableList.sortedCopyOf(
        map.keySet().stream()
            .map(keyPattern::matcher)
            .filter(Matcher::find)
            .map(match -> Long.parseLong(match.group(1)))
            .collect(ImmutableSet.toImmutableSet()));
  }

  // Get an (optionally present) enum value from dynamic form map formData, with key
  // expectedFieldName.
  private <T extends Enum<T>> Optional<T> optionallyGetEnumFromFormData(
      Class<T> enumClass, ImmutableMap<String, String> formData, String expectedFieldName) {
    if (!formData.containsKey(expectedFieldName)) {
      return Optional.empty();
    }

    String scalarFieldValue = formData.get(expectedFieldName);
    return getIfPresent(enumClass, scalarFieldValue).transform(Optional::of).or(Optional.empty());
  }

  /**
   * Given an {@link ImmutableList} of {@link EditConditionPartialViewModel}, focus the subcondition
   * at conditions[conditionIndex].subconditions[subconditionIndex].
   *
   * @param conditions The list of conditions to be edited.
   * @param conditionIndex The (zero-indexed) condition whose subcondition we'd like to focus.
   * @param subconditionIndex The (zero-indexed) subcondition we'd like to focus.
   * @param shouldAnnounceChanges Controls whether these changes will be announced via aria-live.
   */
  private ImmutableList<EditConditionPartialViewModel> focusSubconditionInList(
      ImmutableList<EditConditionPartialViewModel> conditions,
      int conditionIndex,
      int subconditionIndex,
      boolean shouldAnnounceChanges) {
    // Get the focused elements from their respective lists.
    EditConditionPartialViewModel focusedCondition = conditions.get(conditionIndex);
    ArrayList<EditSubconditionPartialViewModel> focusedSubconditionList =
        new ArrayList<>(focusedCondition.subconditions());
    EditSubconditionPartialViewModel focusedSubcondition =
        focusedSubconditionList.get(subconditionIndex).toBuilder()
            .autofocus(true)
            .shouldAnnounceChanges(shouldAnnounceChanges)
            .build();

    // Set correct element
    focusedSubconditionList.set(subconditionIndex, focusedSubcondition);
    focusedCondition =
        focusedCondition.toBuilder()
            .subconditions(ImmutableList.copyOf(focusedSubconditionList))
            .build();

    ArrayList<EditConditionPartialViewModel> conditionsArrayList = new ArrayList<>(conditions);
    conditionsArrayList.set(conditionIndex, focusedCondition);

    return ImmutableList.copyOf(conditionsArrayList);
  }

  /**
   * Given an {@link ImmutableList} of {@link EditConditionPartialViewModel}, return true if any
   * subconditions have invalid input fields present.
   */
  private static boolean predicateHasInvalidInputs(
      ImmutableList<EditConditionPartialViewModel> conditions) {
    return conditions.stream()
        .anyMatch(
            condition ->
                condition.subconditions().stream()
                    .anyMatch(subcondition -> !subcondition.invalidInputIds().isEmpty()));
  }
}
