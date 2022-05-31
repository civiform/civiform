package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import forms.BlockVisibilityPredicateForm;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import views.admin.programs.ProgramBlockPredicatesEditView;

/** Controller for admins editing and viewing program show-hide logic. */
public class AdminProgramBlockPredicatesController extends CiviFormController {
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramBlockPredicatesEditView predicatesEditView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramBlockPredicatesController(
      ProgramService programService,
      QuestionService questionService,
      ProgramBlockPredicatesEditView predicatesEditView,
      FormFactory formFactory,
      RequestChecker requestChecker) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.predicatesEditView = checkNotNull(predicatesEditView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
  }

  /**
   * Return a HTML page containing current show-hide configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);
      return ok(
          predicatesEditView.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailablePredicateQuestionDefinitions(blockDefinitionId)));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  /** POST endpoint for updating show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    Form<BlockVisibilityPredicateForm> predicateFormWrapper =
        formFactory.form(BlockVisibilityPredicateForm.class).bindFromRequest(request);

    if (predicateFormWrapper.hasErrors()) {
      StringBuilder errorMessageBuilder = new StringBuilder("Did not save visibility condition:");
      predicateFormWrapper
          .errors()
          .forEach(error -> errorMessageBuilder.append(String.format("\n• %s", error.message())));

      return redirect(
              routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
          .flashing("error", errorMessageBuilder.toString());
    } else {
      // TODO(https://github.com/seattle-uat/civiform/issues/322): Implement complex predicates.
      //  Right now we only support "leaf node" predicates (a single logical statement based on one
      //  question). In the future we should support logical statements that combine multiple "leaf
      //  node" predicates with ANDs and ORs.
      BlockVisibilityPredicateForm predicateForm = predicateFormWrapper.get();

      Scalar scalar = Scalar.valueOf(predicateForm.getScalar());
      Operator operator = Operator.valueOf(predicateForm.getOperator());
      PredicateValue predicateValue =
          parsePredicateValue(
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
        programService.setBlockPredicate(programId, blockDefinitionId, predicateDefinition);
      } catch (ProgramNotFoundException e) {
        return notFound(String.format("Program ID %d not found.", programId));
      } catch (ProgramBlockDefinitionNotFoundException e) {
        return notFound(
            String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
      } catch (IllegalPredicateOrderingException e) {
        return redirect(
                routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
            .flashing("error", e.getLocalizedMessage());
      }

      ReadOnlyQuestionService roQuestionService =
          questionService.getReadOnlyQuestionService().toCompletableFuture().join();

      return redirect(
              routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
          .flashing(
              "success",
              String.format(
                  "Saved visibility condition: %s %s",
                  action.toDisplayString(),
                  leafExpression.toDisplayString(roQuestionService.getUpToDateQuestions())));
    }
  }

  /** POST endpoint for deleting show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroy(long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeBlockPredicate(programId, blockDefinitionId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(routes.AdminProgramBlockPredicatesController.edit(programId, blockDefinitionId))
        .flashing("success", "Removed the visibility condition for this screen.");
  }

  /**
   * Parses the given value based on the given scalar type and operator. For example, if the scalar
   * is of type LONG and the operator is of type ANY_OF, the value will be parsed as a list of
   * comma-separated longs.
   *
   * <p>If value is the empty string, then parses the list of values instead.
   */
  private PredicateValue parsePredicateValue(
      Scalar scalar, Operator operator, String value, List<String> values) {

    // If the scalar is SELECTION or SELECTIONS then this is a multi-option question predicate, and
    // the right hand side values are in the `values` list rather than the `value` string.
    if (scalar == Scalar.SELECTION || scalar == Scalar.SELECTIONS) {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      return PredicateValue.listOfStrings(builder.addAll(values).build());
    }

    switch (scalar.toScalarType()) {
      case DATE:
        LocalDate localDate = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return PredicateValue.of(localDate);

      case LONG:
        switch (operator) {
          case IN:
          case NOT_IN:
            ImmutableList<Long> listOfLongs =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(s -> Long.parseLong(s.trim()))
                    .collect(ImmutableList.toImmutableList());
            return PredicateValue.listOfLongs(listOfLongs);
          default: // EQUAL_TO, NOT_EQUAL_TO, GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN,
            // LESS_THAN_OR_EQUAL_TO
            return PredicateValue.of(Long.parseLong(value));
        }

      default: // STRING - we list all operators here, but in reality only IN and NOT_IN are
        // expected. The others are handled using the "values" field in the predicate form
        switch (operator) {
          case ANY_OF:
          case IN:
          case NONE_OF:
          case NOT_IN:
          case SUBSET_OF:
            ImmutableList<String> listOfStrings =
                Splitter.on(",")
                    .splitToStream(value)
                    .map(s -> s.trim())
                    .collect(ImmutableList.toImmutableList());
            return PredicateValue.listOfStrings(listOfStrings);
          default: // EQUAL_TO, NOT_EQUAL_TO
            return PredicateValue.of(value);
        }
    }
  }
}
