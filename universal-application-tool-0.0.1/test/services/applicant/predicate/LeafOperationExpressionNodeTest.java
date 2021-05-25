package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;

public class LeafOperationExpressionNodeTest {

  @Test
  public void create() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(node.getType()).isEqualTo(PredicateExpressionNodeType.LEAF_OPERATION);
    assertThat(node.questionId()).isEqualTo(1L);
    assertThat(node.scalar()).isEqualTo(Scalar.CITY);
    assertThat(node.operator()).isEqualTo(Operator.EQUAL_TO);
    assertThat(node.comparedValue()).isEqualTo(PredicateValue.of("Seattle"));
    assertThat(node.toJsonPathPredicate(Path.create("applicant.address")))
        .isEqualTo(JsonPathPredicate.create("$.applicant.address[?(@.city == \"Seattle\")]"));
  }

  @Test
  public void toJsonPathPredicate_canBeEvaluated() {
    ApplicantData data = new ApplicantData();
    data.putString(Path.create("applicant.address.city"), "Chicago");

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Chicago"));
    JsonPathPredicate predicate = node.toJsonPathPredicate(Path.create("applicant.address"));

    assertThat(data.evalPredicate(predicate)).isTrue();
  }
}
