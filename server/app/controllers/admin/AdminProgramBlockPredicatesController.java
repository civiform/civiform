package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import featureflags.FeatureFlags;
import forms.BlockVisibilityPredicateForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateGenerator;
import services.program.predicate.PredicateValue;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramBlockPredicateConfigureView;
import views.admin.programs.ProgramBlockPredicatesEditView;
import views.admin.programs.ProgramBlockPredicatesEditView.ViewType;
import views.admin.programs.ProgramBlockPredicatesEditViewV2;

/**
 * Controller for admins editing and viewing program predicates for eligibility and visibility
 * logic.
 */
public class AdminProgramBlockPredicatesController extends CiviFormController {
  private final PredicateGenerator predicateGenerator;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramBlockPredicatesEditView predicatesEditView;
  private final ProgramBlockPredicatesEditViewV2 predicatesEditViewV2;
  private final ProgramBlockPredicateConfigureView predicatesConfigureView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final FeatureFlags featureFlags;

  @Inject
  public AdminProgramBlockPredicatesController(
      PredicateGenerator predicateGenerator,
      ProgramService programService,
      QuestionService questionService,
      ProgramBlockPredicatesEditView predicatesEditView,
      ProgramBlockPredicatesEditViewV2 predicatesEditViewV2,
      ProgramBlockPredicateConfigureView predicatesConfigureView,
      FormFactory formFactory,
      RequestChecker requestChecker,
      FeatureFlags featureFlags) {
    this.predicateGenerator = checkNotNull(predicateGenerator);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.predicatesEditView = checkNotNull(predicatesEditView);
    this.predicatesEditViewV2 = checkNotNull(predicatesEditViewV2);
    this.predicatesConfigureView = checkNotNull(predicatesConfigureView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.featureFlags = checkNotNull(featureFlags);
  }

  /**
   * Return an HTML page containing current show-hide configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      if (featureFlags.isPredicatesMultipleQuestionsEnabled(request)) {
        return ok(
            predicatesEditViewV2.render(
                request,
                programDefinition,
                blockDefinition,
                programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(
                    blockDefinitionId),
                ProgramBlockPredicatesEditViewV2.ViewType.VISIBILITY));
      }

      return ok(
          predicatesEditView.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(
                  blockDefinitionId),
              ViewType.VISIBILITY));

    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  /**
   * Return an HTML page containing current eligibility configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editEligibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      if (featureFlags.isPredicatesMultipleQuestionsEnabled(request)) {
        return ok(
            predicatesEditViewV2.render(
                request,
                programDefinition,
                blockDefinition,
                programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(
                    blockDefinitionId),
                ProgramBlockPredicatesEditViewV2.ViewType.ELIGIBILITY));
      }

      return ok(
          predicatesEditView.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(
                  blockDefinitionId),
              ViewType.ELIGIBILITY));

    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }
  /** POST endpoint for updating show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    if (featureFlags.isPredicatesMultipleQuestionsEnabled(request)) {
      try {
        PredicateDefinition predicateDefinition =
            predicateGenerator.generatePredicateDefinition(
                programService.getProgramDefinition(programId),
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
            .flashing("error", e.getLocalizedMessage());
      }

      return redirect(
              routes.AdminProgramBlockPredicatesController.editVisibility(
                  programId, blockDefinitionId))
          .flashing("success", "Saved visibility condition");
    }

    Form<BlockVisibilityPredicateForm> predicateFormWrapper =
        formFactory.form(BlockVisibilityPredicateForm.class).bindFromRequest(request);

    if (predicateFormWrapper.hasErrors()) {
      StringBuilder errorMessageBuilder = new StringBuilder("Did not save visibility condition:");
      predicateFormWrapper
          .errors()
          .forEach(error -> errorMessageBuilder.append(String.format("\n• %s", error.message())));

      return redirect(
              routes.AdminProgramBlockPredicatesController.editVisibility(
                  programId, blockDefinitionId))
          .flashing("error", errorMessageBuilder.toString());
    }

    BlockVisibilityPredicateForm predicateForm = predicateFormWrapper.get();

    Scalar scalar = Scalar.valueOf(predicateForm.getScalar());
    Operator operator = Operator.valueOf(predicateForm.getOperator());
    PredicateValue predicateValue =
        PredicateGenerator.parsePredicateValue(
            scalar,
            operator,
            predicateForm.getPredicateValue(),
            predicateForm.getPredicateValues());

    LeafOperationExpressionNode leafExpression =
        LeafOperationExpressionNode.create(
            predicateForm.getQuestionId(), scalar, operator, predicateValue);
    PredicateAction action = PredicateAction.valueOf(predicateForm.getPredicateAction());
    PredicateDefinition predicateDefinition =
        PredicateDefinition.create(PredicateExpressionNode.create(leafExpression), action);

    try {
      programService.setBlockVisibilityPredicate(
          programId, blockDefinitionId, Optional.of(predicateDefinition));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editVisibility(
                  programId, blockDefinitionId))
          .flashing("error", e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editVisibility(
                programId, blockDefinitionId))
        .flashing(
            "success",
            String.format(
                "Saved visibility condition: %s %s",
                action.toDisplayString(),
                leafExpression.toDisplayString(roQuestionService.getUpToDateQuestions())));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureNewVisibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesConfigureView.renderNewVisibility(
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
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
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
          predicatesConfigureView.renderExistingVisibility(
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
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesConfigureView.renderNewEligibility(
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
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
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
          predicatesConfigureView.renderExistingEligibility(
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

    if (featureFlags.isPredicatesMultipleQuestionsEnabled(request)) {
      try {
        EligibilityDefinition eligibility =
            EligibilityDefinition.builder()
                .setPredicate(
                    predicateGenerator.generatePredicateDefinition(
                        programService.getProgramDefinition(programId),
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
          | ProgramQuestionDefinitionNotFoundException e) {
        return redirect(
                routes.AdminProgramBlockPredicatesController.editEligibility(
                    programId, blockDefinitionId))
            .flashing("error", e.getLocalizedMessage());
      }

      return redirect(
              routes.AdminProgramBlockPredicatesController.editEligibility(
                  programId, blockDefinitionId))
          .flashing("success", "Saved eligibility condition");
    }

    Form<BlockVisibilityPredicateForm> predicateFormWrapper =
        formFactory.form(BlockVisibilityPredicateForm.class).bindFromRequest(request);

    if (predicateFormWrapper.hasErrors()) {
      StringBuilder errorMessageBuilder = new StringBuilder("Did not save eligibility condition:");
      predicateFormWrapper
          .errors()
          .forEach(error -> errorMessageBuilder.append(String.format("\n• %s", error.message())));

      return redirect(
              routes.AdminProgramBlockPredicatesController.editEligibility(
                  programId, blockDefinitionId))
          .flashing("error", errorMessageBuilder.toString());
    }

    BlockVisibilityPredicateForm predicateForm = predicateFormWrapper.get();

    Scalar scalar = Scalar.valueOf(predicateForm.getScalar());
    Operator operator = Operator.valueOf(predicateForm.getOperator());
    PredicateValue predicateValue =
        PredicateGenerator.parsePredicateValue(
            scalar,
            operator,
            predicateForm.getPredicateValue(),
            predicateForm.getPredicateValues());

    LeafOperationExpressionNode leafExpression =
        LeafOperationExpressionNode.create(
            predicateForm.getQuestionId(), scalar, operator, predicateValue);
    PredicateAction action = PredicateAction.valueOf(predicateForm.getPredicateAction());
    PredicateDefinition predicateDefinition =
        PredicateDefinition.create(PredicateExpressionNode.create(leafExpression), action);

    try {
      programService.setBlockEligibilityDefinition(
          programId,
          blockDefinitionId,
          Optional.of(EligibilityDefinition.builder().setPredicate(predicateDefinition).build()));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editEligibility(
                  programId, blockDefinitionId))
          .flashing("error", e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editEligibility(
                programId, blockDefinitionId))
        .flashing(
            "success",
            String.format(
                "Saved eligibility condition: %s %s",
                action.toDisplayString(),
                leafExpression.toDisplayString(roQuestionService.getUpToDateQuestions())));
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
        .flashing("success", "Removed the visibility condition for this screen.");
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
        .flashing("success", "Removed the eligibility condition for this screen.");
  }
}
