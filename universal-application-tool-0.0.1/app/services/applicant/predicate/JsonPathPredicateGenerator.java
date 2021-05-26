package services.applicant.predicate;

import services.Path;
import services.program.predicate.LeafOperationExpressionNode;

public class JsonPathPredicateGenerator {

  /**
   * Formats a {@link LeafOperationExpressionNode} in JsonPath format: {@code path[?(expression)]}
   *
   * <p>Example: \$.applicant.address[?(@.zip in ["12345", "56789"])]
   */
  public JsonPathPredicate fromLeafNode(LeafOperationExpressionNode node, Path questionPath) {
    return JsonPathPredicate.create(
        String.format(
            "%s[?(@.%s %s %s)]",
            questionPath.predicateFormat(),
            node.scalar().name().toLowerCase(),
            node.operator().toJsonPathOperator(),
            node.comparedValue().value()));
  }
}
