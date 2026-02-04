package services.applicant.predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.ObjectMapperSingleton;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.MapSelection;
import services.program.predicate.AndNode;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Evaluates complex predicates based on the given {@link ApplicantData}. */
public final class PredicateEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(PredicateEvaluator.class);

  private final ApplicantData applicantData;
  private final JsonPathPredicateGenerator predicateGenerator;

  public PredicateEvaluator(
      ApplicantData applicantData, JsonPathPredicateGenerator predicateGenerator) {
    this.applicantData = applicantData;
    this.predicateGenerator = predicateGenerator;
  }

  /**
   * Evaluate an expression tree rooted at the given {@link PredicateExpressionNode}. Will return
   * true if and only if the entire tree evaluates to true based on the {@link ApplicantData} used
   * to create this evaluator.
   */
  public boolean evaluate(PredicateExpressionNode node) {
    return switch (node.getType()) {
      case LEAF_OPERATION -> evaluateLeafNode(node.getLeafOperationNode());
      case LEAF_ADDRESS_SERVICE_AREA ->
          evaluateLeafAddressServiceAreaNode(node.getLeafAddressNode());
      case AND -> evaluateAndNode(node.getAndNode());
      case OR -> evaluateOrNode(node.getOrNode());
    };
  }

  /**
   * Returns true if and only if there exists a value in {@link ApplicantData} that satisfies the
   * given leaf node operation. Returns false if the predicate is invalid or the predicate evaluates
   * to false.
   */
  private boolean evaluateLeafNode(LeafOperationExpressionNode node) {
    try {
      JsonPathPredicate predicate = predicateGenerator.fromLeafNode(node);

      QuestionDefinition questionDefinition =
          predicateGenerator.getQuestionDefinition(node.questionId());
      if (questionDefinition != null
          && questionDefinition.getQuestionType().equals(QuestionType.MAP)) {
        return evaluateMapQuestionLeafNode(node, applicantData, predicate);
      }

      return applicantData.evalPredicate(predicate);
    } catch (InvalidPredicateException e) {
      logger.error(
          "InvalidPredicateException when evaluating LeafOperationExpressionNode {}: {}",
          node,
          e.getMessage());
      return false;
    }
  }

  /**
   * Returns true if and only if the answer for the address question referenced by the {@link
   * LeafAddressServiceAreaExpressionNode} has an in-area or failed service area in {@link
   * ApplicantData}.
   */
  private boolean evaluateLeafAddressServiceAreaNode(LeafAddressServiceAreaExpressionNode node) {
    try {
      JsonPathPredicate predicate = predicateGenerator.fromLeafAddressServiceAreaNode(node);
      return applicantData.evalPredicate(predicate);
    } catch (InvalidPredicateException e) {
      logger.error(
          "InvalidPredicateException when evaluating LeafAddressServiceAreaExpressionNode {}: {}",
          node,
          e.getMessage());
      return false;
    }
  }

  /** Returns true if and only if each of the node's children evaluates to true. */
  private boolean evaluateAndNode(AndNode node) {
    return node.children().stream().allMatch(this::evaluate);
  }

  /** Returns true if and only if one or more of the node's children evaluates to true. */
  private boolean evaluateOrNode(OrNode node) {
    return node.children().stream().anyMatch(this::evaluate);
  }

  /**
   * Map questions store selections as JSON strings containing both featureId and locationName. This
   * method extracts just the feature IDs and creates a flattened ApplicantData copy for predicate
   * evaluation.
   *
   * @param node the leaf operation expression node for the map question
   * @param applicantData the original applicant data containing {@link MapSelection} JSON
   * @param predicate the predicate to evaluate against the flattened data
   * @return true if the predicate matches any of the flattened feature IDs
   * @throws InvalidPredicateException if path generation fails or predicate evaluation fails
   */
  private boolean evaluateMapQuestionLeafNode(
      LeafOperationExpressionNode node, ApplicantData applicantData, JsonPathPredicate predicate)
      throws InvalidPredicateException {
    Path questionPath = predicateGenerator.getPath(node);
    Path selectionsPath = questionPath.join(node.scalar());

    ApplicantData flattenedData = new ApplicantData(applicantData.asJsonString());

    ImmutableList<String> featureIds =
        applicantData.readStringList(selectionsPath).stream()
            .flatMap(Collection::stream)
            .map(
                jsonString -> {
                  try {
                    return ObjectMapperSingleton.instance()
                        .readValue(jsonString, MapSelection.class)
                        .featureId();
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    flattenedData.putArray(selectionsPath, featureIds);

    return flattenedData.evalPredicate(predicate);
  }
}
