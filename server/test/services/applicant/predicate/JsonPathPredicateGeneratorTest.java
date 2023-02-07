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
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
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
    generator = new JsonPathPredicateGenerator(ImmutableList.of(question), Optional.empty());
  }

  @Test
  public void fromLeafNode_nonRepeatedQuestion_generatesCorrectFormat() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            question.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));

    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create("$.applicant.applicant_address[?(@.city == \"Seattle\")]"));
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForArrayValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            question.getId(),
            Scalar.CITY,
            Operator.IN,
            PredicateValue.listOfStrings(ImmutableList.of("Seattle", "Portland")));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_address[?(@.city in [\"Seattle\", \"Portland\"])]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putString(Path.create("applicant.applicant_address.city"), "Portland");

    assertThat(data.evalPredicate(predicate)).isTrue();
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

    // Put an entity at the enumerator path, so the entity is generated.
    ApplicantQuestion applicantEnumerator =
        new ApplicantQuestion(enumerator, applicantData, Optional.empty());
    applicantData.putRepeatedEntities(
        applicantEnumerator.getContextualizedPath(), ImmutableList.of("Xylia"));

    ImmutableList<RepeatedEntity> repeatedEntities =
        RepeatedEntity.createRepeatedEntities(enumerator, Optional.empty(), applicantData);
    Optional<RepeatedEntity> repeatedEntity = repeatedEntities.stream().findFirst();

    // The block repeated entity context is the one for the repeated name question.
    generator =
        new JsonPathPredicateGenerator(
            ImmutableList.of(enumerator, repeatedQuestion), repeatedEntity);

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            enumerator.getId(), // The predicate is based on the parent enumerator.
            Scalar.FIRST_NAME,
            Operator.EQUAL_TO,
            PredicateValue.of("Xylia"));

    // The top-level enumerator should not appear with [], since it is not repeated.
    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create(
                "$.applicant.applicant_household_members[?(@.first_name == \"Xylia\")]"));
  }

  @Test
  public void fromLeafNode_predicateBasedOnSiblingRepeatedQuestion_generatesCorrectPath()
      throws Exception {
    ApplicantData applicantData = new ApplicantData();
    EnumeratorQuestionDefinition enumerator =
        (EnumeratorQuestionDefinition)
            questionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition siblingQuestion =
        new QuestionDefinitionBuilder(
                questionBank.applicantHouseholdMemberName().getQuestionDefinition())
            .setEnumeratorId(Optional.of(enumerator.getId()))
            .build();

    // Put an entity at the enumerator path so we can generate repeated contexts.
    ApplicantQuestion applicantEnumerator =
        new ApplicantQuestion(enumerator, applicantData, Optional.empty());
    applicantData.putRepeatedEntities(
        applicantEnumerator.getContextualizedPath(), ImmutableList.of("Bernard", "Alice"));

    // Just use a repeated entity for the first (index 0) entity.
    ImmutableList<RepeatedEntity> repeatedEntities =
        RepeatedEntity.createRepeatedEntities(enumerator, Optional.empty(), applicantData);
    Optional<RepeatedEntity> repeatedEntity = repeatedEntities.stream().findFirst();

    generator =
        new JsonPathPredicateGenerator(
            ImmutableList.of(enumerator, siblingQuestion), repeatedEntity);

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            siblingQuestion.getId(), // The predicate is based on the sibling "name" question.
            Scalar.FIRST_NAME,
            Operator.EQUAL_TO,
            PredicateValue.of("Bernard"));

    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create(
                "$.applicant.applicant_household_members[0].household_members_name"
                    + "[?(@.first_name == \"Bernard\")]"));
  }

  @Test
  public void fromLeafNode_twoLevelsDeepRepeater_generatesCorrectPath() throws Exception {
    ApplicantData applicantData = new ApplicantData();
    // household members
    //  \_ name (target), jobs
    //                      \_ days worked (current block)
    QuestionDefinition topLevelEnumerator =
        questionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition targetQuestion =
        questionBank.applicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition nestedEnumerator =
        questionBank.applicantHouseholdMemberJobs().getQuestionDefinition();
    QuestionDefinition currentQuestion =
        questionBank.applicantHouseholdMemberDaysWorked().getQuestionDefinition();

    // Put an entity at the enumerator path so we can generate repeated contexts.
    ApplicantQuestion applicantEnumerator =
        new ApplicantQuestion(topLevelEnumerator, applicantData, Optional.empty());
    applicantData.putRepeatedEntities(
        applicantEnumerator.getContextualizedPath(), ImmutableList.of("Bernard", "Alice"));
    // Context for index 1 ('Alice')
    Optional<RepeatedEntity> topLevelRepeatedEntity =
        RepeatedEntity.createRepeatedEntities(
                (EnumeratorQuestionDefinition) topLevelEnumerator, Optional.empty(), applicantData)
            .reverse()
            .stream()
            .findFirst();

    // Create entities for the nested enumerator
    ApplicantQuestion nestedApplicantEnumerator =
        new ApplicantQuestion(nestedEnumerator, applicantData, topLevelRepeatedEntity);
    applicantData.putRepeatedEntities(
        nestedApplicantEnumerator.getContextualizedPath(), ImmutableList.of("Software Engineer"));
    Optional<RepeatedEntity> currentContext =
        topLevelRepeatedEntity
            .get()
            .createNestedRepeatedEntities(
                (EnumeratorQuestionDefinition) nestedEnumerator, Optional.empty(), applicantData)
            .stream()
            .findFirst();

    generator =
        new JsonPathPredicateGenerator(
            ImmutableList.of(topLevelEnumerator, nestedEnumerator, targetQuestion, currentQuestion),
            currentContext);

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            targetQuestion.getId(), // The predicate is based on the "name" question.
            Scalar.FIRST_NAME,
            Operator.EQUAL_TO,
            PredicateValue.of("Alice"));

    assertThat(generator.fromLeafNode(node))
        .isEqualTo(
            JsonPathPredicate.create(
                "$.applicant.applicant_household_members[1].household_members_name"
                    + "[?(@.first_name == \"Alice\")]"));
  }

  @Test
  public void fromLeafNode_targetQuestionNotAncestorOfCurrentContext_throwsException()
      throws Exception {
    ApplicantData applicantData = new ApplicantData();
    QuestionDefinition topLevelEnumerator =
        questionBank.applicantHouseholdMembers().getQuestionDefinition();
    QuestionDefinition targetQuestion =
        new QuestionDefinitionBuilder(
                questionBank.applicantHouseholdMemberName().getQuestionDefinition())
            .setEnumeratorId(Optional.of(12345L))
            .build();
    QuestionDefinition currentQuestion =
        questionBank.applicantHouseholdMemberJobs().getQuestionDefinition();

    // Put an entity at the enumerator path so we can generate repeated contexts.
    ApplicantQuestion applicantEnumerator =
        new ApplicantQuestion(topLevelEnumerator, applicantData, Optional.empty());
    applicantData.putRepeatedEntities(
        applicantEnumerator.getContextualizedPath(), ImmutableList.of("Alice"));
    // Context for 'Alice'
    Optional<RepeatedEntity> currentContext =
        RepeatedEntity.createRepeatedEntities(
                (EnumeratorQuestionDefinition) topLevelEnumerator, Optional.empty(), applicantData)
            .reverse()
            .stream()
            .findFirst();

    generator =
        new JsonPathPredicateGenerator(
            ImmutableList.of(targetQuestion, currentQuestion), currentContext);

    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            targetQuestion.getId(), // The predicate is based on the "name" question.
            Scalar.FIRST_NAME,
            Operator.EQUAL_TO,
            PredicateValue.of("Alice"));

    assertThatThrownBy(() -> generator.fromLeafNode(node))
        .isInstanceOf(InvalidPredicateException.class)
        .hasMessageContaining(
            "Enumerator 12345 is not an ancestor of the current repeated context");
  }

  @Test
  public void fromLeafServiceAreaNode_generatesCorrectPredicate() throws Exception {
    ApplicantData data = new ApplicantData();
    data.putString(
        Path.create("applicant.applicant_address.service_area"),
        "bloomington_Failed_1234,king-county_InArea_2222,seattle_InArea_5678,Arkansas_NotInArea_8765");

    JsonPathPredicate predicate =
        generator.fromLeafAddressServiceAreaNode(
            LeafAddressServiceAreaExpressionNode.create(question.getId(), "seattle"));
    assertThat(data.evalPredicate(predicate)).isTrue();

    predicate =
        generator.fromLeafAddressServiceAreaNode(
            LeafAddressServiceAreaExpressionNode.create(question.getId(), "bloomington"));
    assertThat(data.evalPredicate(predicate)).isTrue();

    predicate =
        generator.fromLeafAddressServiceAreaNode(
            LeafAddressServiceAreaExpressionNode.create(question.getId(), "king-county"));
    assertThat(data.evalPredicate(predicate)).isTrue();

    predicate =
        generator.fromLeafAddressServiceAreaNode(
            LeafAddressServiceAreaExpressionNode.create(question.getId(), "Arkansas"));
    assertThat(data.evalPredicate(predicate)).isFalse();

    predicate =
        generator.fromLeafAddressServiceAreaNode(
            LeafAddressServiceAreaExpressionNode.create(question.getId(), "Kansas"));
    assertThat(data.evalPredicate(predicate)).isFalse();
  }

  @Test
  public void fromLeafServiceAreaNode_invalidServiceAreaId_throws() {
    assertThatThrownBy(
            () ->
                generator.fromLeafAddressServiceAreaNode(
                    LeafAddressServiceAreaExpressionNode.create(question.getId(), "busted ID")))
        .isInstanceOf(InvalidPredicateException.class);
  }
}
