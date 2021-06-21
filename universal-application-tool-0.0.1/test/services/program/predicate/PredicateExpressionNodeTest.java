package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import services.applicant.question.Scalar;

public class PredicateExpressionNodeTest {

  @Test
  public void getQuestions_leaf() {
    LeafOperationExpressionNode leaf =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));

    PredicateExpressionNode node = PredicateExpressionNode.create(leaf);

    assertThat(node.getQuestions()).containsExactly(123L);
  }

  @Test
  public void getQuestions_and() {
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            456L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    AndNode and =
        AndNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(and);

    assertThat(node.getQuestions()).containsExactly(123L, 456L);
  }

  @Test
  public void getQuestions_or() {
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            456L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    OrNode or =
        OrNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(or);

    assertThat(node.getQuestions()).containsExactly(123L, 456L);
  }
}
