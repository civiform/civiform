package services.applicant.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.DateConverter;
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

  private Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00.00Z"), ZoneId.of("UTC"));
  private final TestQuestionBank questionBank = new TestQuestionBank(false);
  private QuestionDefinition question;
  private QuestionDefinition dateQuestion;
  private QuestionDefinition numberQuestion;
  private QuestionDefinition currencyQuestion;
  private JsonPathPredicateGenerator generator;

  @Before
  public void setupGenerator() {
    question = questionBank.applicantAddress().getQuestionDefinition();
    dateQuestion = questionBank.applicantDate().getQuestionDefinition();
    numberQuestion = questionBank.applicantJugglingNumber().getQuestionDefinition();
    currencyQuestion = questionBank.applicantIceCream().getQuestionDefinition();
    generator =
        new JsonPathPredicateGenerator(
            new DateConverter(clock),
            ImmutableList.of(question, dateQuestion, numberQuestion, currencyQuestion),
            Optional.empty());
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
  public void fromLeafNode_generatesCorrectStringForAgeOlderValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(), Scalar.DATE, Operator.AGE_OLDER_THAN, PredicateValue.of(18));

    JsonPathPredicate predicate =
        JsonPathPredicate.create("$.applicant.applicant_birth_date[?(1325376000000 >= @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2012-01-01");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForAgeOlderDoubleValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(),
            Scalar.DATE,
            Operator.AGE_OLDER_THAN,
            PredicateValue.of((double) 18.5));

    JsonPathPredicate predicate =
        JsonPathPredicate.create("$.applicant.applicant_birth_date[?(1309478400000 >= @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2011-07-01");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForAgeYoungerValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(), Scalar.DATE, Operator.AGE_YOUNGER_THAN, PredicateValue.of(18));

    JsonPathPredicate predicate =
        JsonPathPredicate.create("$.applicant.applicant_birth_date[?(1325376000000 < @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2012-01-02");
    assertThat(data.evalPredicate(predicate)).isTrue();

    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2012-01-01");
    assertThat(data.evalPredicate(predicate)).isFalse();
  }

  @Test
  public void fromLeafNode_evaluatesCorrectlyWhenAgeNotInPredicate() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(), Scalar.DATE, Operator.AGE_OLDER_THAN, PredicateValue.of(18));

    // This person will be 100 sometime in the future, which is why the timestamp is negative.
    JsonPathPredicate predicate =
        JsonPathPredicate.create("$.applicant.applicant_birth_date[?(1325376000000 >= @.date)]");

    assertThat(generator.fromLeafNode(node).pathPredicate()).isEqualTo(predicate.pathPredicate());

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2022-01-02");

    assertThat(data.evalPredicate(predicate)).isFalse();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenAgeListValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(),
            Scalar.DATE,
            Operator.AGE_BETWEEN,
            PredicateValue.listOfLongs(ImmutableList.of(1L, 100L)));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_birth_date[?(1861920000000 >= @.date && -1262304000000 <="
                + " @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2022-01-01");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenAgePairValue() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(),
            Scalar.DATE,
            Operator.AGE_BETWEEN,
            PredicateValue.pairOfLongs(1, 100));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_birth_date[?(1861920000000 >= @.date && -1262304000000 <="
                + " @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2022-01-01");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenAgeValue_wrongOrder() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(),
            Scalar.DATE,
            Operator.AGE_BETWEEN,
            PredicateValue.listOfLongs(ImmutableList.of(100L, 1L)));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_birth_date[?(1861920000000 >= @.date && -1262304000000 <="
                + " @.date)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putDate(Path.create("applicant.applicant_birth_date.date"), "2022-01-01");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenNumber() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            numberQuestion.getId(),
            Scalar.NUMBER,
            Operator.BETWEEN,
            PredicateValue.pairOfLongs(0, 20));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.number_of_items_applicant_can_juggle[?(0 <= @.number && @.number <= 20)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putLong(Path.create("applicant.number_of_items_applicant_can_juggle.number"), "5");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenCurrency() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            currencyQuestion.getId(),
            Scalar.CURRENCY_CENTS,
            Operator.BETWEEN,
            PredicateValue.pairOfLongs(300, 1000));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_ice_cream[?(300 <= @.currency_cents && @.currency_cents <="
                + " 1000)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putLong(Path.create("applicant.applicant_ice_cream.currency_cents"), "550");

    assertThat(data.evalPredicate(predicate)).isTrue();
  }

  @Test
  public void fromLeafNode_generatesCorrectStringForBetweenDate() throws Exception {
    LeafOperationExpressionNode node =
        LeafOperationExpressionNode.create(
            dateQuestion.getId(),
            Scalar.DATE,
            Operator.BETWEEN,
            PredicateValue.pairOfDates(LocalDate.of(2020, 5, 20), LocalDate.of(2024, 5, 20)));

    JsonPathPredicate predicate =
        JsonPathPredicate.create(
            "$.applicant.applicant_birth_date[?(1589932800000 <= @.date && @.date <="
                + " 1716163200000)]");

    assertThat(generator.fromLeafNode(node)).isEqualTo(predicate);

    ApplicantData data = new ApplicantData();
    data.putLong(
        Path.create("applicant.applicant_birth_date.date"), "1653004800000"); // May 20, 2022

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
            Mockito.mock(DateConverter.class),
            ImmutableList.of(enumerator, repeatedQuestion),
            repeatedEntity);

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
            Mockito.mock(DateConverter.class),
            ImmutableList.of(enumerator, siblingQuestion),
            repeatedEntity);

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
            Mockito.mock(DateConverter.class),
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
            Mockito.mock(DateConverter.class),
            ImmutableList.of(targetQuestion, currentQuestion),
            currentContext);

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
