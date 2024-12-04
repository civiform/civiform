package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.DateConverter;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class PredicateEvaluatorTest {

  private final TestQuestionBank questionBank = new TestQuestionBank(false);
  private final QuestionDefinition addressQuestion =
      questionBank.addressApplicantAddress().getQuestionDefinition();

  private ApplicantData applicantData;
  private ApplicantQuestion applicantQuestion;
  private JsonPathPredicateGenerator generator;
  private PredicateEvaluator evaluator;

  @Before
  public void setupEvaluator() {
    applicantData = new ApplicantData();
    applicantQuestion =
        new ApplicantQuestion(
            addressQuestion, new ApplicantModel(), applicantData, Optional.empty());
    generator =
        new JsonPathPredicateGenerator(
            Mockito.mock(DateConverter.class), ImmutableList.of(addressQuestion), Optional.empty());
    evaluator = new PredicateEvaluator(applicantData, generator);
  }

  @Test
  public void evaluate_leafNode_correctlyEvaluatesValidPredicate() {
    LeafOperationExpressionNode leafNode =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(),
            Scalar.STREET,
            Operator.EQUAL_TO,
            PredicateValue.of("123 Rhode St."));
    PredicateExpressionNode node = PredicateExpressionNode.create(leafNode);

    applicantData.putString(
        applicantQuestion.createAddressQuestion().getStreetPath(), "123 Rhode St.");
    assertThat(evaluator.evaluate(node)).isTrue();

    applicantData.putString(applicantQuestion.createAddressQuestion().getStreetPath(), "different");
    assertThat(evaluator.evaluate(node)).isFalse();
  }

  @Test
  public void evaluate_leafNode_returnsFalseForInvalidPredicate() {
    LeafOperationExpressionNode leafNode =
        LeafOperationExpressionNode.create(
            addressQuestion.getId() + 1, // Invalid question ID
            Scalar.STREET,
            Operator.EQUAL_TO,
            PredicateValue.of("123 Rhode St."));
    PredicateExpressionNode node = PredicateExpressionNode.create(leafNode);

    applicantData.putString(
        applicantQuestion.createAddressQuestion().getStreetPath(), "123 Rhode St.");

    assertThat(evaluator.evaluate(node)).isFalse();
  }

  @Test
  public void evaluate_andNode_returnsTrue() {
    LeafOperationExpressionNode city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    LeafOperationExpressionNode state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Seattle");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "WA");

    AndNode andNode =
        AndNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(andNode))).isTrue();
  }

  @Test
  public void evaluate_andNode_oneFalse_returnsFalse() {
    LeafOperationExpressionNode city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    LeafOperationExpressionNode state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Spokane");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "WA");

    AndNode andNode =
        AndNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(andNode))).isFalse();
  }

  @Test
  public void evaluate_orNode_oneTrue_returnsTrue() {
    LeafOperationExpressionNode city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    LeafOperationExpressionNode state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Spokane");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "WA");

    OrNode orNode =
        OrNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(orNode))).isTrue();
  }

  @Test
  public void evaluate_orNode_allFalse_returnsFalse() {
    LeafOperationExpressionNode city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    LeafOperationExpressionNode state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Minneapolis");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "MN");

    OrNode orNode =
        OrNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(orNode))).isFalse();
  }

  @Test
  public void evaluate_andOrTree_returnsTrue() {
    var city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    var state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));
    var zip =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.ZIP, Operator.EQUAL_TO, PredicateValue.of("55555"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Spokane");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "WA");
    applicantData.putString(applicantQuestion.createAddressQuestion().getZipPath(), "55555");

    OrNode orNode =
        OrNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));
    AndNode andNode =
        AndNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(orNode), PredicateExpressionNode.create(zip)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(andNode))).isTrue();
  }

  @Test
  public void evaluate_andOrTree_andFails_returnsFalse() {
    var city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    var state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));
    var zip =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.ZIP, Operator.EQUAL_TO, PredicateValue.of("55555"));

    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "Spokane");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "WA");
    // Mismatched AND data.
    applicantData.putString(applicantQuestion.createAddressQuestion().getZipPath(), "NOT 55555");

    OrNode orNode =
        OrNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));
    AndNode andNode =
        AndNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(orNode), PredicateExpressionNode.create(zip)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(andNode))).isFalse();
  }

  @Test
  public void evaluate_andOrTree_orFails_returnsFalse() {
    var city =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.CITY, Operator.EQUAL_TO, PredicateValue.of("Seattle"));
    var state =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.STATE, Operator.EQUAL_TO, PredicateValue.of("WA"));
    var zip =
        LeafOperationExpressionNode.create(
            addressQuestion.getId(), Scalar.ZIP, Operator.EQUAL_TO, PredicateValue.of("55555"));

    // Mismatched Or data.
    applicantData.putString(applicantQuestion.createAddressQuestion().getCityPath(), "NOT Spokane");
    applicantData.putString(applicantQuestion.createAddressQuestion().getStatePath(), "NOT WA");
    applicantData.putString(applicantQuestion.createAddressQuestion().getZipPath(), "55555");

    OrNode orNode =
        OrNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(city), PredicateExpressionNode.create(state)));
    AndNode andNode =
        AndNode.create(
            ImmutableList.of(
                PredicateExpressionNode.create(orNode), PredicateExpressionNode.create(zip)));

    assertThat(evaluator.evaluate(PredicateExpressionNode.create(andNode))).isFalse();
  }
}
