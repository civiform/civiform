package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class BlockDefinitionTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void createBlockDefinition() {
    BlockDefinition block =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .build();

    assertThat(block.id()).isEqualTo(123L);
  }

  @Test
  public void isEnumerator_isFalse() {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isEnumerator()).isFalse();
  }

  @Test
  public void isRepeated_isFalse() {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isRepeated()).isFalse();
  }

  @Test
  public void isFileUpload_isFalse() {
    BlockDefinition blockDefinition = makeBlockDefinitionWithQuestions();

    assertThat(blockDefinition.isFileUpload()).isFalse();
  }

  @Test
  public void isEnumerator_isTrue() {
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
                    Optional.empty()))
            .build();

    assertThat(blockDefinition.isEnumerator()).isTrue();
  }

  @Test
  public void isRepeated_isTrue() {
    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder().setEnumeratorId(Optional.of(1L)).build();

    assertThat(blockDefinition.isRepeated()).isTrue();
  }

  @Test
  public void isFileUpload_isTrue() {
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(
                ProgramQuestionDefinition.create(
                    testQuestionBank.applicantFile().getQuestionDefinition(), Optional.empty()))
            .build();

    assertThat(blockDefinition.isFileUpload()).isTrue();
  }

  @Test
  public void setAndGetVisibilityPredicate() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder().setVisibilityPredicate(predicate).build();

    assertThat(blockDefinition.visibilityPredicate()).hasValue(predicate);
  }

  @Test
  public void setAndGetEligibilityDefinition() {
    var visibilityAddress = LeafAddressServiceAreaExpressionNode.create(1L, "");
    PredicateDefinition visibilityPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(visibilityAddress), PredicateAction.HIDE_BLOCK);

    var eligibilityAddress = LeafAddressServiceAreaExpressionNode.create(2L, "");
    PredicateDefinition eligibilityPredicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(eligibilityAddress), PredicateAction.HIDE_BLOCK);
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder().setPredicate(eligibilityPredicate).build();

    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder()
            .setEligibilityDefinition(eligibility)
            .setVisibilityPredicate(visibilityPredicate)
            .build();

    assertThat(blockDefinition.getAddressServiceAreaPredicateNodes())
        .containsExactlyInAnyOrder(visibilityAddress, eligibilityAddress);
    assertThat(blockDefinition.hasAddressServiceAreaPredicateNodes()).isTrue();
  }

  @Test
  public void getAddressServiceAreaPredicateNodes() {
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of(""))),
            PredicateAction.HIDE_BLOCK);
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder().setPredicate(predicate).build();

    BlockDefinition blockDefinition =
        makeBlockDefinitionWithQuestions().toBuilder()
            .setEligibilityDefinition(eligibility)
            .build();

    assertThat(blockDefinition.eligibilityDefinition()).hasValue(eligibility);
  }

  @Test
  public void getAddressCorrectionEnabledOnDifferentQuestion() {
    QuestionDefinition firstAddress = testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition secondAddress =
        testQuestionBank.applicantSecondaryAddress().getQuestionDefinition();
    ProgramQuestionDefinition firstQuestion =
        ProgramQuestionDefinition.create(firstAddress, Optional.empty());
    // Second address has correction enabled
    ProgramQuestionDefinition secondQuestion =
        ProgramQuestionDefinition.create(secondAddress, Optional.empty(), false, true);
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(firstQuestion)
            .addQuestion(secondQuestion)
            .build();

    assertThat(blockDefinition.hasAddressCorrectionEnabledOnDifferentQuestion(firstAddress.getId()))
        .isTrue();
    assertThat(
            blockDefinition.hasAddressCorrectionEnabledOnDifferentQuestion(secondAddress.getId()))
        .isFalse();
  }

  private BlockDefinition makeBlockDefinitionWithQuestions() {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    QuestionDefinition colorQuestion =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();

    BlockDefinition block =
        BlockDefinition.builder()
            .setId(123L)
            .setName("Block Name")
            .setDescription("Block Description")
            .addQuestion(ProgramQuestionDefinition.create(nameQuestion, Optional.empty()))
            .addQuestion(ProgramQuestionDefinition.create(addressQuestion, Optional.empty()))
            .addQuestion(ProgramQuestionDefinition.create(colorQuestion, Optional.empty()))
            .build();
    return block;
  }
}
