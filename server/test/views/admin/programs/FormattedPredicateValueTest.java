package views.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateValue;

public class FormattedPredicateValueTest {

  @Test
  public void format_currencyEqualTo() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.CURRENCY_CENTS)
            .setOperator(Operator.EQUAL_TO)
            .setComparedValue(PredicateValue.of(1234))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("12.34"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_currencyBetween() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.CURRENCY_CENTS)
            .setOperator(Operator.BETWEEN)
            .setComparedValue(PredicateValue.pairOfLongs(1234, 5678))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("12.34"), Optional.of("56.78"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_dateEqualTo() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.EQUAL_TO)
            .setComparedValue(PredicateValue.of(LocalDate.of(2024, 5, 20)))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("2024-05-20"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_dateBetween() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.BETWEEN)
            .setComparedValue(
                PredicateValue.pairOfDates(LocalDate.of(2020, 5, 20), LocalDate.of(2024, 5, 20)))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("2020-05-20"), Optional.of("2024-05-20"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_dateAgeBetween_legacy() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.AGE_BETWEEN)
            .setComparedValue(PredicateValue.listOfLongs(ImmutableList.of(18L, 30L)))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("18"), Optional.of("30"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_dateAgeBetween() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.AGE_BETWEEN)
            .setComparedValue(PredicateValue.pairOfLongs(18, 30))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("18"), Optional.of("30"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_ageYounger() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.AGE_YOUNGER_THAN)
            .setComparedValue(PredicateValue.of(30))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("30"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_ageOlderDecimal() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.AGE_OLDER_THAN)
            .setComparedValue(PredicateValue.of(30.5))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("30.5"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_dateEqual() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.DATE)
            .setOperator(Operator.EQUAL_TO)
            .setComparedValue(PredicateValue.of(LocalDate.of(2024, 5, 20)))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("2024-05-20"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_longLessThan() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.NUMBER)
            .setOperator(Operator.LESS_THAN)
            .setComparedValue(PredicateValue.of(30))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("30"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_longBetween() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.NUMBER)
            .setOperator(Operator.BETWEEN)
            .setComparedValue(PredicateValue.pairOfLongs(30, 50))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("30"), Optional.of("50"));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_stringEqualTo() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.FIRST_NAME)
            .setOperator(Operator.EQUAL_TO)
            .setComparedValue(PredicateValue.of("a"))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("a"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_stringIn() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.FIRST_NAME)
            .setOperator(Operator.IN)
            .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("a", "b", "c")))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("a,b,c"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void format_listOfStringsAnyOf() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.builder()
            .setQuestionId(1)
            .setScalar(Scalar.SELECTIONS)
            .setOperator(Operator.ANY_OF)
            .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("a", "b", "c")))
            .build();
    FormattedPredicateValue actual = FormattedPredicateValue.fromLeafNode(Optional.of(node));
    FormattedPredicateValue expected =
        FormattedPredicateValue.create(Optional.of("a,b,c"), Optional.empty());
    assertThat(actual).isEqualTo(expected);
  }
}
