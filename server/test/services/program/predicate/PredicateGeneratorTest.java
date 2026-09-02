package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinitionNotFoundException;
import support.CfTestHelpers;
import support.ProgramBuilder;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class PredicateGeneratorTest extends ResetPostgres {
  private FormFactory formFactory;
  private PredicateGenerator predicateGenerator;
  private TestQuestionBank testQuestionBank = new TestQuestionBank(/* canSave= */ false);
  private ProgramDefinition programDefinition =
      ProgramBuilder.newDraftProgram("program1")
          .withBlock()
          .withRequiredQuestion(testQuestionBank.numberApplicantJugglingNumber())
          .withRequiredCorrectedAddressQuestion(testQuestionBank.addressApplicantAddress())
          .withRequiredQuestion(testQuestionBank.dateApplicantBirthdate())
          .withBlock()
          .withRequiredQuestion(testQuestionBank.checkboxApplicantKitchenTools())
          .buildDefinition();

  @Before
  public void setUp() {
    formFactory = instanceOf(FormFactory.class);
    predicateGenerator = instanceOf(PredicateGenerator.class);
  }

  @Test
  public void singleQuestion_singleValue_currency() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "CURRENCY_CENTS")
                .put("condition-1-subcondition-1-operator", "GREATER_THAN")
                .put("condition-1-subcondition-1-value", "12.34")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());
    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.CURRENCY_CENTS)
                    .setOperator(Operator.GREATER_THAN)
                    .setComparedValue(PredicateValue.of(1234))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_currencyBetween() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "CURRENCY_CENTS")
                .put("condition-1-subcondition-1-operator", "BETWEEN")
                .put("condition-1-subcondition-1-value", "12.34")
                .put("condition-1-subcondition-1-secondValue", "56.78")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.CURRENCY_CENTS)
                    .setOperator(Operator.BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(1234, 5678))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_dateBetween() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "BETWEEN")
                .put("condition-1-subcondition-1-value", "2020-05-20")
                .put("condition-1-subcondition-1-secondValue", "2024-05-20")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
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
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "IS_AFTER")
                .put("condition-1-subcondition-1-value", "2024-05-20")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.IS_AFTER)
                    .setComparedValue(PredicateValue.of(LocalDate.of(2024, 5, 20)))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageBetween() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "AGE_BETWEEN")
                .put("condition-1-subcondition-1-value", "14")
                .put("condition-1-subcondition-1-secondValue", "18")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(14, 18))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageOlderThan() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "AGE_OLDER_THAN")
                .put("condition-1-subcondition-1-value", "18")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_OLDER_THAN)
                    .setComparedValue(PredicateValue.of(18))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_ageYoungerThanDouble() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "AGE_YOUNGER_THAN")
                .put("condition-1-subcondition-1-value", "10.5")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.DATE)
                    .setOperator(Operator.AGE_YOUNGER_THAN)
                    .setComparedValue(PredicateValue.of(10.5))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_serviceArea() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.addressApplicantAddress().id.toString())
                .put("condition-1-subcondition-1-scalar", Scalar.SERVICE_AREAS.name())
                .put("condition-1-subcondition-1-operator", Operator.IN_SERVICE_AREA.name())
                .put("condition-1-subcondition-1-value", "seattle")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.addressApplicantAddress().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafAddressServiceAreaExpressionNode.create(
                    testQuestionBank.addressApplicantAddress().id,
                    "seattle",
                    Operator.IN_SERVICE_AREA)));
  }

  @Test
  public void singleQuestion_singleValue_numberIn() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "NUMBER")
                .put("condition-1-subcondition-1-operator", "IN")
                .put("condition-1-subcondition-1-value", "1,2,3")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.IN)
                    .setComparedValue(PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L)))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_numberBetween() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "NUMBER")
                .put("condition-1-subcondition-1-operator", "BETWEEN")
                .put("condition-1-subcondition-1-value", "1234")
                .put("condition-1-subcondition-1-secondValue", "5678")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.BETWEEN)
                    .setComparedValue(PredicateValue.pairOfLongs(1234, 5678))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_numberGreaterThan() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "NUMBER")
                .put("condition-1-subcondition-1-operator", "GREATER_THAN")
                .put("condition-1-subcondition-1-value", "1234")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.NUMBER)
                    .setOperator(Operator.GREATER_THAN)
                    .setComparedValue(PredicateValue.of(1234))
                    .build()));
  }

  @Test
  public void singleQuestion_singleValue_lastNameEquals() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "LAST_NAME")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "abcdef")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
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
                "root-node-type",
                "OR",
                "condition-1-node-type",
                "AND",
                "condition-1-subcondition-1-question",
                testQuestionBank.dateApplicantBirthdate().id.toString(),
                "condition-1-subcondition-1-scalar",
                "FIRST_NAME",
                "condition-1-subcondition-1-operator",
                "IN",
                "condition-1-subcondition-1-value",
                "a,b,c"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.dateApplicantBirthdate().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                    .setScalar(Scalar.FIRST_NAME)
                    .setOperator(Operator.IN)
                    .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("a", "b", "c")))
                    .build()));
  }

  @Test
  public void multiCondition_OR_multiSubcondition_AND() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.<String, String>builder()
                .put("predicateAction", "SHOW_BLOCK")
                .put("root-node-type", "OR")
                .put("condition-1-node-type", "AND")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "2023-01-01")
                .put(
                    "condition-1-subcondition-2-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-2-scalar", "NUMBER")
                .put("condition-1-subcondition-2-operator", "EQUAL_TO")
                .put("condition-1-subcondition-2-value", "1")
                .put("condition-2-node-type", "AND")
                .put(
                    "condition-2-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-2-subcondition-1-scalar", "DATE")
                .put("condition-2-subcondition-1-operator", "EQUAL_TO")
                .put("condition-2-subcondition-1-value", "2023-02-02")
                .put(
                    "condition-2-subcondition-2-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-2-subcondition-2-scalar", "NUMBER")
                .put("condition-2-subcondition-2-operator", "EQUAL_TO")
                .put("condition-2-subcondition-2-value", "2")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.MULTIPLE_CONDITIONS);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.numberApplicantJugglingNumber().id,
            testQuestionBank.dateApplicantBirthdate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.OR);
    assertThat(predicateDefinition.rootNode().getOrNode().children())
        .containsExactlyInAnyOrder(
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(1))
                                .build())))),
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-02-02"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(2))
                                .build())))));
  }

  @Test
  public void multiCondition_AND_multiSubcondition_AND_OR() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.<String, String>builder()
                .put("predicateAction", "SHOW_BLOCK")
                .put("root-node-type", "AND")
                .put("condition-1-node-type", "AND")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "2023-01-01")
                .put(
                    "condition-1-subcondition-2-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-2-scalar", "NUMBER")
                .put("condition-1-subcondition-2-operator", "EQUAL_TO")
                .put("condition-1-subcondition-2-value", "1")
                .put("condition-2-node-type", "OR")
                .put(
                    "condition-2-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-2-subcondition-1-scalar", "DATE")
                .put("condition-2-subcondition-1-operator", "EQUAL_TO")
                .put("condition-2-subcondition-1-value", "2023-02-02")
                .put(
                    "condition-2-subcondition-2-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-2-subcondition-2-scalar", "NUMBER")
                .put("condition-2-subcondition-2-operator", "EQUAL_TO")
                .put("condition-2-subcondition-2-value", "2")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.MULTIPLE_CONDITIONS);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.numberApplicantJugglingNumber().id,
            testQuestionBank.dateApplicantBirthdate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.AND);
    assertThat(predicateDefinition.rootNode().getAndNode().children())
        .containsExactlyInAnyOrder(
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(1))
                                .build())))),
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-02-02"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(2))
                                .build())))));
  }

  @Test
  public void singleCondition_AND_multiSubcondition_OR() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.<String, String>builder()
                .put("predicateAction", "SHOW_BLOCK")
                .put("root-node-type", "AND")
                .put("condition-1-node-type", "OR")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "2023-01-01")
                .put(
                    "condition-1-subcondition-2-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-2-scalar", "NUMBER")
                .put("condition-1-subcondition-2-operator", "EQUAL_TO")
                .put("condition-1-subcondition-2-value", "1")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.numberApplicantJugglingNumber().id,
            testQuestionBank.dateApplicantBirthdate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.AND);
    assertThat(predicateDefinition.rootNode().getAndNode().children())
        .containsExactly(
            PredicateExpressionNode.create(
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(1))
                                .build())))));
  }

  @Test
  public void multiCondition_OR_singleSubcondition_AND() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.<String, String>builder()
                .put("predicateAction", "SHOW_BLOCK")
                .put("root-node-type", "OR")
                .put("condition-1-node-type", "AND")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.dateApplicantBirthdate().id.toString())
                .put("condition-1-subcondition-1-scalar", "DATE")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "2023-01-01")
                .put("condition-2-node-type", "AND")
                .put(
                    "condition-2-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-2-subcondition-1-scalar", "NUMBER")
                .put("condition-2-subcondition-1-operator", "EQUAL_TO")
                .put("condition-2-subcondition-1-value", "1")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.MULTIPLE_CONDITIONS);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.numberApplicantJugglingNumber().id,
            testQuestionBank.dateApplicantBirthdate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.OR);
    assertThat(predicateDefinition.rootNode().getOrNode().children())
        .containsExactly(
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.dateApplicantBirthdate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(CfTestHelpers.stringToPredicateDate("2023-01-01"))
                                .build())))),
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.numberApplicantJugglingNumber().id)
                                .setScalar(Scalar.NUMBER)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of(1))
                                .build())))));
  }

  @Test
  public void multiselect() throws Exception {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.checkboxApplicantKitchenTools().id.toString())
                .put("condition-1-subcondition-1-scalar", "SELECTION")
                .put("condition-1-subcondition-1-operator", "ANY_OF")
                .put("condition-1-subcondition-1-values[0]", "1")
                .put("condition-1-subcondition-1-values[1]", "2")
                .build());

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(programDefinition, form, fakeRequest());

    assertThat(predicateDefinition.predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_CONDITION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.checkboxApplicantKitchenTools().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.checkboxApplicantKitchenTools().id)
                    .setScalar(Scalar.SELECTION)
                    .setOperator(Operator.ANY_OF)
                    .setComparedValue(PredicateValue.listOfStrings(ImmutableList.of("1", "2")))
                    .build()));
  }

  @Test
  public void invalidServiceArea_throws() {
    // Scalar is service area but question is not address question
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-1-scalar", Scalar.SERVICE_AREAS.name())
                .put("condition-1-subcondition-1-operator", Operator.IN_SERVICE_AREA.name())
                .put("condition-1-subcondition-1-value", "seattle")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void invalidQuestionId() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.emailApplicantEmail().id.toString())
                .put("condition-1-subcondition-1-scalar", "EMAIL")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "98144")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(ProgramQuestionDefinitionNotFoundException.class);
  }

  @Test
  public void invalidAction() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("invalid")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-1-scalar", "NUMBER")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "98144")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void missingScalar() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "1")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void invalidScalar() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-1-scalar", "invalid")
                .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                .put("condition-1-subcondition-1-value", "1")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void missingOperator() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.addressApplicantAddress().id.toString())
                .put("condition-1-subcondition-1-scalar", "ZIP")
                .put("condition-1-subcondition-1-value", "1")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void invalidOperator() {
    DynamicForm form =
        buildForm(
            getExpandedFormBuilder("HIDE_BLOCK")
                .put(
                    "condition-1-subcondition-1-question",
                    testQuestionBank.numberApplicantJugglingNumber().id.toString())
                .put("condition-1-subcondition-1-scalar", "NUMBER")
                .put("condition-1-subcondition-1-operator", "invalid")
                .put("condition-1-subcondition-1-value", "98144")
                .build());

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form, fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  private ImmutableMap.Builder<String, String> getExpandedFormBuilder(String action) {
    return ImmutableMap.<String, String>builder()
        .put("predicateAction", action)
        .put("root-node-type", "OR")
        .put("condition-1-node-type", "AND");
  }

  private DynamicForm buildForm(ImmutableMap<String, String> formContents) {
    return formFactory.form().bindFromRequest(fakeRequestBuilder().bodyForm(formContents).build());
  }
}
