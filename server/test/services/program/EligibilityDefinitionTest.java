package services.program;

import static org.assertj.core.api.Assertions.assertThat;

import models.QuestionModel;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import support.TestQuestionBank;

public class EligibilityDefinitionTest {

  private static final TestQuestionBank testQuestionBank =
      new TestQuestionBank(/* canSave= */ false);

  @Test
  public void setAndGet() {
    QuestionModel predicateQuestion = testQuestionBank.textApplicantFavoriteColor();
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    predicateQuestion.id,
                    Scalar.TEXT,
                    Operator.EQUAL_TO,
                    PredicateValue.of("yellow"))),
            PredicateAction.SHOW_BLOCK);

    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(predicate).build();

    assertThat(eligibilityDefinition.predicate()).isEqualTo(predicate);
  }
}
