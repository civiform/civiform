package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

  @Test
  public void toDisplayString_leafNodeOnly() {
    LeafOperationExpressionNode leaf =
        LeafOperationExpressionNode.create(
            123L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(PredicateExpressionNode.create(leaf).toDisplayString())
        .isEqualTo("city is equal to \"Seattle\"");
  }

  @Test
  public void toDisplayString_andNode() {
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.NUMBER, Operator.GREATER_THAN, PredicateValue.of(45));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            456L, Scalar.NUMBER, Operator.LESS_THAN_OR_EQUAL_TO, PredicateValue.of(72));
    AndNode and =
        AndNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(and);

    assertThat(node.toDisplayString())
        .isEqualTo("number is greater than 45 and number is less than or equal to 72");
  }

  @Test
  public void toDisplayString_orNode() {
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.ANY_OF, PredicateValue.of(ImmutableList.of("a", "b")));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            456L,
            Scalar.DATE,
            Operator.IS_BEFORE,
            PredicateValue.of(
                LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    OrNode or =
        OrNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(or);

    assertThat(node.toDisplayString())
        .isEqualTo(
            "selection contains any of [\"a\", \"b\"] or date is earlier than 2021-01-01");
  }
}
