package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import support.CfTestHelpers;
import support.FakeReadOnlyQuestionService;
import support.ProgramBuilder;
import support.TestQuestionBank;

public class PredicateGeneratorTest extends ResetPostgres {
  private FormFactory formFactory;
  private PredicateGenerator predicateGenerator;
  private TestQuestionBank testQuestionBank = new TestQuestionBank(/* canSave= */ false);
  private ProgramDefinition programDefinition =
      ProgramBuilder.newDraftProgram("program1")
          .withBlock()
          .withRequiredQuestion(testQuestionBank.applicantJugglingNumber())
          .withRequiredCorrectedAddressQuestion(testQuestionBank.applicantAddress())
          .withRequiredQuestion(testQuestionBank.applicantDate())
          .withBlock()
          .withRequiredQuestion(testQuestionBank.applicantKitchenTools())
          .buildDefinition();
  private ReadOnlyQuestionService readOnlyQuestionService =
      new FakeReadOnlyQuestionService(
          ImmutableList.of(
              testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
              testQuestionBank.applicantAddress().getQuestionDefinition(),
              testQuestionBank.applicantDate().getQuestionDefinition(),
              testQuestionBank.applicantKitchenTools().getQuestionDefinition()));

  @Before
  public void setUp() {
    formFactory = instanceOf(FormFactory.class);
    predicateGenerator = instanceOf(PredicateGenerator.class);
  }

  @Test
  public void singleQuestion_singleValue_currency() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "CURRENCY_CENTS",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "GREATER_THAN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "12.34"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.CURRENCY_CENTS)
                    .setOperator(Operator.GREATER_THAN)
                    .setComparedValue(PredicateValue.of(1234))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_currencyBetween() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "CURRENCY_CENTS",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "BETWEEN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "12.34",
                String.format(
                    "group-1-question-%d-predicateSecondValue",
                    testQuestionBank.applicantDate().id),
                "56.78"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.CURRENCY_CENTS)
                    .setOperator(Operator.BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(1234, 5678))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_dateBetween() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "BETWEEN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "2020-05-20",
                String.format(
                    "group-1-question-%d-predicateSecondValue",
                    testQuestionBank.applicantDate().id),
                "2024-05-20"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.BETWEEN)
                    .setComparedValue(
                        PredicateValue.pairOfDates(
                            LocalDate.of(2020, 5, 20), LocalDate.of(2024, 5, 20)))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_dateIsAfter() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "IS_AFTER",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "2024-05-20"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.IS_AFTER)
                    .setComparedValue(PredicateValue.of(LocalDate.of(2024, 5, 20)))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageBetween() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "AGE_BETWEEN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "14",
                String.format(
                    "group-1-question-%d-predicateSecondValue",
                    testQuestionBank.applicantDate().id),
                "18"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(14, 18))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageOlderThan() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "AGE_OLDER_THAN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "18"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_OLDER_THAN)
                    .setComparedValue(PredicateValue.of(18))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageYoungerThanDouble() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "AGE_YOUNGER_THAN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "10.5"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_YOUNGER_THAN)
                    .setComparedValue(PredicateValue.of(10.5))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_serviceArea() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "SERVICE_AREA",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "IN_SERVICE_AREA",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "seattle"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantAddress().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafAddressServiceAreaExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantAddress().id)
                    .setServiceAreaId("seattle")
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_serviceArea_invalidId_throws() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "SERVICE_AREA",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "IN_SERVICE_AREA",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "seattle invalid"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void singleQuestion_singleValue_numberIn() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "NUMBER",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "IN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "1,2,3"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.IN)
                    .setComparedValue(PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L)))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_numberBetween() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "NUMBER",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "BETWEEN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "1234",
                String.format(
                    "group-1-question-%d-predicateSecondValue",
                    testQuestionBank.applicantDate().id),
                "5678"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(1234, 5678))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_numberGreaterThan() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "NUMBER",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "GREATER_THAN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "1234"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.GREATER_THAN)
                    .setComparedValue(PredicateValue.of(1234))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_lastNameEquals() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "LAST_NAME",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "abcdef"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.LAST_NAME)
                    .setOperator(Operator.EQUAL_TO)
                    .setComparedValue(PredicateValue.of("abcdef"))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_firstNameIn() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "FIRST_NAME",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "IN",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "a,b,c"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantDate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantDate().id)
                    .setScalar(Scalar.FIRST_NAME)
                    .setOperator(Operator.IN)
                    .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("a", "b", "c")))
                    .build()));
  }

  @Test
  public void multiQuestion_multiValue() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "SHOW_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantJugglingNumber().id),
                "NUMBER",
                String.format(
                    "question-%d-operator", testQuestionBank.applicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "1",
                String.format(
                    "group-2-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "2",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "2023-01-01",
                String.format(
                    "group-2-question-%d-predicateValue", testQuestionBank.applicantDate().id),
                "2023-02-02"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.applicantJugglingNumber().id, testQuestionBank.applicantDate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.OR);
    assertThat(predicateDefinition.rootNode().getOrNode().children())
        .containsExactlyInAnyOrder(
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantDate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(1))
                                .build())))),
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantDate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-02-02"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(2))
                                .build())))));
  }

  @Test
  public void multiselect() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantKitchenTools().id),
                "SELECTION",
                String.format("question-%d-operator", testQuestionBank.applicantKitchenTools().id),
                "ANY_OF",
                String.format(
                    "group-1-question-%d-predicateValues[1]",
                    testQuestionBank.applicantKitchenTools().id),
                "1",
                String.format(
                    "group-1-question-%d-predicateValues[2]",
                    testQuestionBank.applicantKitchenTools().id),
                "2"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantKitchenTools().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantKitchenTools().id)
                    .setScalar(Scalar.SELECTION)
                    .setOperator(Operator.ANY_OF)
                    .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("2", "1")))
                    .build()));
  }

  @Test
  public void invalidQuestionId() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantEmail().id),
                "EMAIL",
                String.format("question-%d-operator", testQuestionBank.applicantEmail().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantEmail().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  @Test
  public void invalidAction() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "invalid",
                String.format("question-%d-scalar", testQuestionBank.applicantJugglingNumber().id),
                "NUMBER",
                String.format(
                    "question-%d-operator", testQuestionBank.applicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void missingScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format(
                    "question-%d-operator", testQuestionBank.applicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void invalidScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantJugglingNumber().id),
                "invalid",
                String.format(
                    "question-%d-operator", testQuestionBank.applicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void missingOperator() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "ZIP",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void invalidOperator() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantJugglingNumber().id),
                "NUMBER",
                String.format(
                    "question-%d-operator", testQuestionBank.applicantJugglingNumber().id),
                "invalid",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.applicantJugglingNumber().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  private DynamicForm buildForm(ImmutableMap<String, String> formContents) {
    return formFactory.form().bindFromRequest(fakeRequest().bodyForm(formContents).build());
  }
}
