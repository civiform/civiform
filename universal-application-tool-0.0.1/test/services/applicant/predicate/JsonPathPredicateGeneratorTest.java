package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class JsonPathPredicateGeneratorTest {

  private QuestionDefinition question;
  private JsonPathPredicateGenerator generator;

  @Before
  public void setupGenerator() {
    TestQuestionBank questionBank = new TestQuestionBank(false);
    question = questionBank.applicantAddress().getQuestionDefinition();
    generator =
        new JsonPathPredicateGenerator(
            ImmutableList.of(
                new ApplicantQuestion(question, new ApplicantData(), Optional.empty())));
  }

  @Test
  public void fromLeafNode_generatesCorrectFormat() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create("$.applicant.applicant_address[?(@.city == \"Seattle\")]"));
  }

  @Test
  public void fromLeafNode_canBeEvaluated() throws Exception {
    ApplicantData data = new ApplicantData();
    data.putString(Path.create("applicant.applicant_address.city"), "Chicago");
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Chicago"));

    JsonPathPredicate predicate = generator.fromLeafNode(node);

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_missingQuestion_throwsInvalidPredicateException() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(123L, Scalar.DATE, Operator.IN, PredicateValue.of(23));

    assertThatThrownBy(() -> generator.fromLeafNode(node))
        .isInstanceOf(InvalidPredicateException.class)
        .hasMessageContaining(
            "Tried to apply a predicate based on question 123, which is not found in this program");
  }
}
