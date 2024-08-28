package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import services.applicant.question.Scalar;

public class PredicateAddressServiceAreaNodeExtractorTest {

  @Test
  public void run_addressServiceAreaNodeExists_returnsTheAddressNodes() {
    var leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    var addressNode =
        LeafAddressServiceAreaExpressionNode.create(456L, "Seattle", Operator.IN_SERVICE_AREA);
    var rootNode =
        PredicateExpressionNode.create(
            OrNode.create(
                ImmutableList.of(
                    PredicateExpressionNode.create(
                        AndNode.create(
                            ImmutableList.of(
                                PredicateExpressionNode.create(leaf1),
                                PredicateExpressionNode.create(addressNode)))))));
    var predicateDefinition = PredicateDefinition.create(rootNode, PredicateAction.HIDE_BLOCK);

    assertThat(PredicateAddressServiceAreaNodeExtractor.extract(predicateDefinition))
        .isEqualTo(ImmutableList.of(addressNode));
  }

  @Test
  public void run_addressServiceAreaNodeDoesNotExist_returnsEmpty() {
    var leaf1 =
        LeafOperationExpressionNode.create(
            123L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("hello"));
    var leaf2 =
        LeafOperationExpressionNode.create(
            456L, Scalar.SELECTION, Operator.EQUAL_TO, PredicateValue.of("goodbye"));
    var rootNode =
        PredicateExpressionNode.create(
            OrNode.create(
                ImmutableList.of(
                    PredicateExpressionNode.create(
                        AndNode.create(
                            ImmutableList.of(
                                PredicateExpressionNode.create(leaf1),
                                PredicateExpressionNode.create(leaf2)))))));
    var predicateDefinition = PredicateDefinition.create(rootNode, PredicateAction.HIDE_BLOCK);

    assertThat(PredicateAddressServiceAreaNodeExtractor.extract(predicateDefinition))
        .isEqualTo(ImmutableList.of());
  }
}
