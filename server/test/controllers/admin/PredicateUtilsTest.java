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
                    Scalar.DATE,
                    Operator.IS_ON_OR_BEFORE,
                    PredicateValue.of(1000L))),
            PredicateAction.SHOW_BLOCK);

    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(
            "My Test Block", predicate, ImmutableList.of());

    assertThat(readablePredicate.heading()).contains("My Test Block");
    assertThat(readablePredicate.heading()).contains("is shown if");
    assertThat(readablePredicate.heading()).contains("is on or earlier than");

    assertThat(readablePredicate.conditionList()).isEmpty();
  }

  @Test
  public void getReadablePredicateDescription_singleAnd_headingOnly() {
    ImmutableList<PredicateExpressionNode> andStatements =
        ImmutableList.of(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    /* questionId= */ 1000,
                    Scalar.DATE,
                    Operator.IS_ON_OR_BEFORE,
                    PredicateValue.of(1000L))),
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

    assertThat(readablePredicate.heading()).contains("My Test Block is hidden if");
    assertThat(readablePredicate.heading()).contains("is on or earlier than");
    assertThat(readablePredicate.heading()).contains("is equal to 4");
    assertThat(readablePredicate.heading()).contains("is not equal to \"hello\"");

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

    // Verify the heading has some introductory text, but not any predicate text
    assertThat(readablePredicate.heading()).contains("My Test Block is eligible if any of:");
    assertThat(readablePredicate.heading()).doesNotContain("is equal to 4");
    assertThat(readablePredicate.heading()).doesNotContain("is equal to 5");

    // Verify there's 1 condition item per AND statement
    assertThat(readablePredicate.conditionList()).isPresent();
    assertThat(readablePredicate.conditionList().get().size()).isEqualTo(2);
    assertThat(readablePredicate.conditionList().get().get(0)).contains("is equal to 4");
    assertThat(readablePredicate.conditionList().get().get(0)).contains("is equal to \"four\"");
    assertThat(readablePredicate.conditionList().get().get(1)).contains("is equal to 5");
    assertThat(readablePredicate.conditionList().get().get(1)).contains("is equal to \"five\"");
  }
}
