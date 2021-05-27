package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.applicant.exception.InvalidPredicateException;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateValue;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import support.TestQuestionBank;

public class JsonPathPredicateGeneratorTest {

  private final TestQuestionBank questionBank = new TestQuestionBank(false);
  private QuestionDefinition question;
  private JsonPathPredicateGenerator generator;

  @Before
  public void setupGenerator() {
    question = questionBank.applicantAddress().getQuestionDefinition();
    generator =
        new JsonPathPredicateGenerator(
            new ApplicantData(), ImmutableList.of(question), Optional.empty());
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

  @Test
  public void fromLeafNode_predicateBasedOnParentEnumerator_generatesCorrectPath()
      throws Exception {
    ApplicantData applicantData = new ApplicantData();
    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition)
            questionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition repeatedQuestion =
        new QuestionDefinitionBuilder(
                questionBank.applicantHouseholdMemberName().getQuestionDefinition())
            .setEnumeratorId(Optional.of(enumerator.getId()))
            .build();
    // I think we need to put an entity at the enumerator path, so the name is generated.
    ApplicantQuestion applicantEnumerator =
        new ApplicantQuestion(enumerator, applicantData, Optional.empty());
    applicantData.putRepeatedEntities(
        applicantEnumerator.getContextualizedPath(), ImmutableList.of("Xylia"));
    System.out.println(applicantData.asJsonString());

    ImmutableList<RepeatedEntity> repeatedEntities =
        RepeatedEntity.createRepeatedEntities(enumerator, applicantData);
    Optional<RepeatedEntity> repeatedEntity = repeatedEntities.stream().findFirst();
    System.out.println(repeatedEntity);

    // The block repeated entity context is the one for the repeated name question.
    generator =
        new JsonPathPredicateGenerator(
            applicantData, ImmutableList.of(enumerator, repeatedQuestion), repeatedEntity);

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            enumerator.getId(), // The predicate is based on the parent enumerator.
            Scalar.FIRST_NAME,
            Operator.EQUAL_TO,
            PredicateValue.of("Xylia"));

    // TODO(future Caroline): you are running into a case where we need the enumerator path without
    // the array reference. getQuestionPathSegment returns it with the array reference.
    // This is fixed by a special case in the generator. NEED TO TEST MORE CASES
    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create(
                "$.applicant.applicant_household_members[?(@.first_name == \"Xylia\")]"));
  }
}
