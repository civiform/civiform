package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import services.applicant.question.Scalar;

public class PredicateDefinitionTest {

  @Test
  public void toDisplayString() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Phoenix"))),
            PredicateAction.HIDE_BLOCK);

    assertThat(predicate.toDisplayString("My Block", ImmutableList.of()))
        .isEqualTo("My Block is hidden if city is equal to \"Phoenix\"");
  }
}
