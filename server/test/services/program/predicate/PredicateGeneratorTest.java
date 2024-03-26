package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
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
  public void generatePredicateDefinition_singleQuestion_singleValue_ageRange() throws Exception {
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
                "14,18"));

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
                    .setComparedValue(
                        PredicateGenerator.parsePredicateValue(
                            Scalar.DATE, Operator.AGE_BETWEEN, "14,18", null))
                    .build()));
  }

  @Test
  public void generatePredicateDefinition_singleQuestion_singleValue_ageRange_invalidRange_throws()
      throws Exception {
    DynamicForm form2 =
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
                "14 18"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form2, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "Invalid age range: 14 18. Age range value must be two integers separated by - or ,");

    DynamicForm form3 =
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
                "14-eighteen"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form3, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "Invalid age range: 14-eighteen. Age range value must be two integers separated by -"
                + " or ,");

    DynamicForm form4 =
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
                "14,18,24"));

    assertThatThrownBy(
            () ->
                predicateGenerator.generatePredicateDefinition(
                    programDefinition, form4, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(
            "Invalid age range: 14,18,24. Age range value must be two integers separated by - or"
                + " ,");
  }
  
  @Test
  public void generatePredicateDefinition_singleQuestion_singleValue_ageLessThan() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantDate().id),
                "DATE",
                String.format("question-%d-operator", testQuestionBank.applicantDate().id),
                "AGE_LESS_THAN",
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
                    .setComparedValue(
                        PredicateGenerator.parsePredicateValue(
                            Scalar.DATE, Operator.AGE_YOUNGER_THAN, "10.5", null))
                    .build()));
  }


  @Test
  public void generatePredicateDefinition_singleQuestion_serviceArea() throws Exception {
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
  public void generatePredicateDefinition_singleQuestion_serviceArea_invalidId_throws()
      throws Exception {
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
  public void generatePredicateDefinition_multiQuestion_multiValue() throws Exception {
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
  public void generatePredicateDefinition_multiselect() throws Exception {
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
  public void generatePredicateDefinition_invalidQuestionId() {
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
  public void generatePredicateDefinition_invalidAction() {
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
  public void generatePredicateDefinition_missingScalar() {
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
  public void generatePredicateDefinition_invalidScalar() {
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
  public void generatePredicateDefinition_missingOperator() {
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
  public void generatePredicateDefinition_invalidOperator() {
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
