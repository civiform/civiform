package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateValue;

public class JsonPathPredicateGeneratorTest {

  private final JsonPathPredicateGenerator generator = new JsonPathPredicateGenerator();

  @Test
  public void fromLeafNode_generatesCorrectFormat() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(generator.fromLeafNode(node, Path.create("applicant.address")))
        .isEqualTo(JsonPathPredicate.create("$.applicant.address[?(@.city == \"Seattle\")]"));
  }

  @Test
  public void fromLeafNode_canBeEvaluated() {
    ApplicantData data = new ApplicantData();
    data.putString(Path.create("applicant.address.city"), "Chicago");
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Chicago"));

    JsonPathPredicate predicate = generator.fromLeafNode(node, Path.create("applicant.address"));

    assertThat(data.evalPredicate(predicate)).isTrue();
  }
}
