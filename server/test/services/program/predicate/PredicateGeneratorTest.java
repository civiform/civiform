package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.settings.SettingsManifest;
import support.CfTestHelpers;
import support.FakeReadOnlyQuestionService;
import support.ProgramBuilder;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class PredicateGeneratorTest extends ResetPostgres {
  private FormFactory formFactory;
  private SettingsManifest settingsManifest;
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
  private ReadOnlyQuestionService readOnlyQuestionService =
      new FakeReadOnlyQuestionService(
          ImmutableList.of(
              testQuestionBank.numberApplicantJugglingNumber().getQuestionDefinition(),
              testQuestionBank.addressApplicantAddress().getQuestionDefinition(),
              testQuestionBank.dateApplicantBirthdate().getQuestionDefinition(),
              testQuestionBank.checkboxApplicantKitchenTools().getQuestionDefinition()));

  @Before
  public void setUp() {
    formFactory = instanceOf(FormFactory.class);
    predicateGenerator = instanceOf(PredicateGenerator.class);
    settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getExpandedFormLogicEnabled()).thenReturn(true);
  }

  @Test
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_currency(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "CURRENCY_CENTS")
                    .put("condition-1-subcondition-1-operator", "GREATER_THAN")
                    .put("condition-1-subcondition-1-value", "12.34")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "CURRENCY_CENTS",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "GREATER_THAN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "12.34"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_currencyBetween(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "CURRENCY_CENTS")
                    .put("condition-1-subcondition-1-operator", "BETWEEN")
                    .put("condition-1-subcondition-1-value", "12.34")
                    .put("condition-1-subcondition-1-secondValue", "56.78")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "CURRENCY_CENTS",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "BETWEEN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "12.34",
                    String.format(
                        "group-1-question-%d-predicateSecondValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "56.78"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_dateBetween(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "DATE")
                    .put("condition-1-subcondition-1-operator", "BETWEEN")
                    .put("condition-1-subcondition-1-value", "2020-05-20")
                    .put("condition-1-subcondition-1-secondValue", "2024-05-20")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "BETWEEN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "2020-05-20",
                    String.format(
                        "group-1-question-%d-predicateSecondValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "2024-05-20"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_dateIsAfter(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "DATE")
                    .put("condition-1-subcondition-1-operator", "IS_AFTER")
                    .put("condition-1-subcondition-1-value", "2024-05-20")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "IS_AFTER",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "2024-05-20"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_ageBetween(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "DATE")
                    .put("condition-1-subcondition-1-operator", "AGE_BETWEEN")
                    .put("condition-1-subcondition-1-value", "14")
                    .put("condition-1-subcondition-1-secondValue", "18")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "AGE_BETWEEN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "14",
                    String.format(
                        "group-1-question-%d-predicateSecondValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "18"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_ageOlderThan(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "DATE")
                    .put("condition-1-subcondition-1-operator", "AGE_OLDER_THAN")
                    .put("condition-1-subcondition-1-value", "18")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "AGE_OLDER_THAN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "18"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_ageYoungerThanDouble(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "DATE")
                    .put("condition-1-subcondition-1-operator", "AGE_YOUNGER_THAN")
                    .put("condition-1-subcondition-1-value", "10.5")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "AGE_YOUNGER_THAN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "10.5"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_serviceArea(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.addressApplicantAddress().id.toString())
                    .put("condition-1-subcondition-1-scalar", Scalar.SERVICE_AREAS.name())
                    .put("condition-1-subcondition-1-operator", Operator.IN_SERVICE_AREA.name())
                    .put("condition-1-subcondition-1-value", "seattle")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.addressApplicantAddress().id),
                    Scalar.SERVICE_AREAS.name(),
                    String.format(
                        "question-%d-operator", testQuestionBank.addressApplicantAddress().id),
                    Operator.IN_SERVICE_AREA.name(),
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.addressApplicantAddress().id),
                    "seattle"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_numberIn(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "NUMBER")
                    .put("condition-1-subcondition-1-operator", "IN")
                    .put("condition-1-subcondition-1-value", "1,2,3")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "NUMBER",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "IN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "1,2,3"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_numberBetween(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "NUMBER")
                    .put("condition-1-subcondition-1-operator", "BETWEEN")
                    .put("condition-1-subcondition-1-value", "1234")
                    .put("condition-1-subcondition-1-secondValue", "5678")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "NUMBER",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "BETWEEN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "1234",
                    String.format(
                        "group-1-question-%d-predicateSecondValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "5678"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_numberGreaterThan(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "NUMBER")
                    .put("condition-1-subcondition-1-operator", "GREATER_THAN")
                    .put("condition-1-subcondition-1-value", "1234")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "NUMBER",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "GREATER_THAN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "1234"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_lastNameEquals(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.dateApplicantBirthdate().id.toString())
                    .put("condition-1-subcondition-1-scalar", "LAST_NAME")
                    .put("condition-1-subcondition-1-operator", "EQUAL_TO")
                    .put("condition-1-subcondition-1-value", "abcdef")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "LAST_NAME",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "EQUAL_TO",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "abcdef"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void singleQuestion_singleValue_firstNameIn(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
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
                    "a,b,c"))
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "FIRST_NAME",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "IN",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "a,b,c"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  @Parameters({"true", "false"})
  public void multiCondition_OR_multiSubcondition_AND(boolean expandedFormLogicEnabled)
      throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
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
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "SHOW_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.numberApplicantJugglingNumber().id),
                    "NUMBER",
                    String.format(
                        "question-%d-operator",
                        testQuestionBank.numberApplicantJugglingNumber().id),
                    "EQUAL_TO",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.numberApplicantJugglingNumber().id),
                    "1",
                    String.format(
                        "group-2-question-%d-predicateValue",
                        testQuestionBank.numberApplicantJugglingNumber().id),
                    "2",
                    String.format(
                        "question-%d-scalar", testQuestionBank.dateApplicantBirthdate().id),
                    "DATE",
                    String.format(
                        "question-%d-operator", testQuestionBank.dateApplicantBirthdate().id),
                    "EQUAL_TO",
                    String.format(
                        "group-1-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "2023-01-01",
                    String.format(
                        "group-2-question-%d-predicateValue",
                        testQuestionBank.dateApplicantBirthdate().id),
                    "2023-02-02"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest());

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
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest());

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
        predicateGenerator.generatePredicateDefinition(
            programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest());

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
  @Parameters({"true", "false"})
  public void multiselect(boolean expandedFormLogicEnabled) throws Exception {
    DynamicForm form =
        expandedFormLogicEnabled
            ? buildForm(
                getExpandedFormBuilder("HIDE_BLOCK")
                    .put(
                        "condition-1-subcondition-1-question",
                        testQuestionBank.checkboxApplicantKitchenTools().id.toString())
                    .put("condition-1-subcondition-1-scalar", "SELECTION")
                    .put("condition-1-subcondition-1-operator", "ANY_OF")
                    .put("condition-1-subcondition-1-values[0]", "1")
                    .put("condition-1-subcondition-1-values[1]", "2")
                    .build())
            : buildForm(
                ImmutableMap.of(
                    "predicateAction",
                    "HIDE_BLOCK",
                    String.format(
                        "question-%d-scalar", testQuestionBank.checkboxApplicantKitchenTools().id),
                    "SELECTION",
                    String.format(
                        "question-%d-operator",
                        testQuestionBank.checkboxApplicantKitchenTools().id),
                    "ANY_OF",
                    String.format(
                        "group-1-question-%d-predicateValues[0]",
                        testQuestionBank.checkboxApplicantKitchenTools().id),
                    "1",
                    String.format(
                        "group-1-question-%d-predicateValues[1]",
                        testQuestionBank.checkboxApplicantKitchenTools().id),
                    "2"));

    PredicateDefinition predicateDefinition =
        expandedFormLogicEnabled
            ? predicateGenerator.generatePredicateDefinition(
                programDefinition, form, readOnlyQuestionService, settingsManifest, fakeRequest())
            : predicateGenerator.legacyGeneratePredicateDefinition(
                programDefinition, form, readOnlyQuestionService);

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
  public void legacy_invalidServiceArea_throws() {
    // Scalar is service area but question is not address question
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format(
                    "question-%d-scalar", testQuestionBank.numberApplicantJugglingNumber().id),
                Scalar.SERVICE_AREAS.name(),
                String.format(
                    "question-%d-operator", testQuestionBank.addressApplicantAddress().id),
                Operator.IN_SERVICE_AREA.name(),
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.addressApplicantAddress().id),
                "seattle"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void legacy_invalidQuestionId() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.emailApplicantEmail().id),
                "EMAIL",
                String.format("question-%d-operator", testQuestionBank.emailApplicantEmail().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.emailApplicantEmail().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(QuestionNotFoundException.class);
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  @Test
  public void legacy_invalidAction() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "invalid",
                String.format(
                    "question-%d-scalar", testQuestionBank.numberApplicantJugglingNumber().id),
                "NUMBER",
                String.format(
                    "question-%d-operator", testQuestionBank.numberApplicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.numberApplicantJugglingNumber().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void legacy_missingScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format(
                    "question-%d-operator", testQuestionBank.numberApplicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.numberApplicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void legacy_invalidScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format(
                    "question-%d-scalar", testQuestionBank.numberApplicantJugglingNumber().id),
                "invalid",
                String.format(
                    "question-%d-operator", testQuestionBank.numberApplicantJugglingNumber().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.numberApplicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void legacy_missingOperator() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.addressApplicantAddress().id),
                "ZIP",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.numberApplicantJugglingNumber().id),
                "1"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void legacy_invalidOperator() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format(
                    "question-%d-scalar", testQuestionBank.numberApplicantJugglingNumber().id),
                "NUMBER",
                String.format(
                    "question-%d-operator", testQuestionBank.numberApplicantJugglingNumber().id),
                "invalid",
                String.format(
                    "group-1-question-%d-predicateValue",
                    testQuestionBank.numberApplicantJugglingNumber().id),
                "98144"));

    assertThatThrownBy(
            () ->
                predicateGenerator.legacyGeneratePredicateDefinition(
                    programDefinition, form, readOnlyQuestionService))
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
                    programDefinition,
                    form,
                    readOnlyQuestionService,
                    settingsManifest,
                    fakeRequest()))
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
