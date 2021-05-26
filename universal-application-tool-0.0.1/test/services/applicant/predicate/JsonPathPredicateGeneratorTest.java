package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

import java.util.Optional;

public class JsonPathPredicateGeneratorTest {

  private QuestionDefinition question;
  private JsonPathPredicateGenerator generator;

  @Before
  public void setupGenerator() {
    TestQuestionBank questionBank = new TestQuestionBank(false);
    question = questionBank.applicantIceCream().getQuestionDefinition();
    generator = new JsonPathPredicateGenerator(ImmutableList.of(new ApplicantQuestion(question, new ApplicantData(), Optional.empty())));
  }

  @Test
  public void fromLeafNode_generatesCorrectFormat() {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(generator.fromLeafNode(node))
        .isEqualTo(JsonPathPredicate.create("$.applicant.applicant address[?(@.city == \"Seattle\")]"));
  }

  @Test
  public void fromLeafNode_canBeEvaluated() {
    ApplicantData data = new ApplicantData();
    data.putString(Path.create("applicant.applicant address.city"), "Chicago");
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            1L, Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Chicago"));

    JsonPathPredicate predicate = generator.fromLeafNode(node);

    assertThat(data.evalPredicate(predicate)).isTrue();
  }
}
