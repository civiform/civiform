package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;

public class PredicateUtilsTest {
  @Test
  public void getReadablePredicateDescription_singleQuestion_headingOnly() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1000,
                    Scalar.NUMBER,
                    Operator.GREATER_THAN_OR_EQUAL_TO,
                    PredicateValue.of(1000L))),
            PredicateAction.SHOW_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of());

    assertThat(readablePredicate.heading())
        .isEqualTo("My Test Block is shown if number is greater than or equal to 1000");
    assertThat(readablePredicate.conditionList()).isEmpty();
  }

  @Test
  public void getReadablePredicateDescription_singleAnd_headingOnly() {
    ImmutableList<PredicateExpressionNode> andStatements =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1000,
                    Scalar.CITY,
                    Operator.EQUAL_TO,
                    PredicateValue.of("Phoenix"))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.LESS_THAN,
                    PredicateValue.of(4))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.NOT_EQUAL_TO,
                    PredicateValue.of("hello"))));
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(AndNode.create(andStatements))))),
            PredicateAction.HIDE_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of());

    assertThat(readablePredicate.heading())
        .isEqualTo(
            "My Test Block is hidden if city is equal to \"Phoenix\" and number is less than 4 and"
                + " text is not equal to \"hello\"");
    assertThat(readablePredicate.conditionList()).isEmpty();
  }

  @Test
  public void getReadablePredicateDescription_multipleAnds_headingAndConditionList() {
    // number == 4 && text == "four"
    ImmutableList<PredicateExpressionNode> andStatements1 =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of(4))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("four"))));
    // number == 5 && text == "five"
    ImmutableList<PredicateExpressionNode> andStatements2 =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1001,
                    Scalar.NUMBER,
                    Operator.EQUAL_TO,
                    PredicateValue.of(5))),
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1002,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("five"))));
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(AndNode.create(andStatements1)),
                        PredicateExpressionNode.create(AndNode.create(andStatements2))))),
            PredicateAction.ELIGIBLE_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of());

    assertThat(readablePredicate.heading()).isEqualTo("My Test Block is eligible if any of:");
    assertThat(readablePredicate.conditionList()).isPresent();
    assertThat(readablePredicate.conditionList().get().size()).isEqualTo(2);
    assertThat(readablePredicate.conditionList().get().get(0))
        .isEqualTo("number is equal to 4 and text is equal to \"four\"");
    assertThat(readablePredicate.conditionList().get().get(1))
        .isEqualTo("number is equal to 5 and text is equal to \"five\"");
  }
}
