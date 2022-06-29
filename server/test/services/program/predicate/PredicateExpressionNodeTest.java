package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class PredicateExpressionNodeTest {

  private final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

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
    QuestionDefinition question = testQuestionBank.applicantAddress().getQuestionDefinition();
    LeafOperationExpressionNode leaf =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(PredicateExpressionNode.create(leaf).toDisplayString(ImmutableList.of(question)))
        .isEqualTo(question.getName() + "'s city is equal to \"Seattle\"");
  }

  @Test
  public void toDisplayString_andNode() {
    QuestionDefinition question =
        testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.NUMBER, Operator.GREATER_THAN, PredicateValue.of(45));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.NUMBER, Operator.LESS_THAN_OR_EQUAL_TO, PredicateValue.of(72));
    AndNode and =
        AndNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(and);

    assertThat(node.toDisplayString(ImmutableList.of(question)))
        .isEqualTo(
            question.getName()
                + "'s number is greater than 45 and "
                + question.getName()
                + "'s number is less than or equal to 72");
  }

  @Test
  public void toDisplayString_orNode() {
    QuestionDefinition multiOption = testQuestionBank.applicantIceCream().getQuestionDefinition();
    QuestionDefinition date = testQuestionBank.applicantDate().getQuestionDefinition();
    LeafOperationExpressionNode leaf1 =
        LeafOperationExpressionNode.create(
            multiOption.getId(),
            Scalar.SELECTION,
            Operator.IN,
            PredicateValue.listOfStrings(ImmutableList.of("1", "2")));
    LeafOperationExpressionNode leaf2 =
        LeafOperationExpressionNode.create(
            date.getId(),
            Scalar.DATE,
            Operator.IS_BEFORE,
            PredicateValue.of(
                LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
    OrNode or =
        OrNode.create(
            ImmutableSet.of(
                PredicateExpressionNode.create(leaf1), PredicateExpressionNode.create(leaf2)));

    PredicateExpressionNode node = PredicateExpressionNode.create(or);

    assertThat(node.toDisplayString(ImmutableList.of(multiOption, date)))
        .isEqualTo(
            multiOption.getName()
                + "'s selection is one of [chocolate, strawberry] or "
                + date.getName()
                + "'s date is earlier than 2021-01-01");
  }
}
