package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.BadRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import support.FakeReadOnlyQuestionService;
import support.TestQuestionBank;

public class PredicateGeneratorTest extends ResetPostgres {

  private FormFactory formFactory;
  private PredicateGenerator predicateGenerator;
  private TestQuestionBank testQuestionBank = new TestQuestionBank(/* canSave= */ false);

  private ReadOnlyQuestionService readOnlyQuestionService =
      new FakeReadOnlyQuestionService(
          ImmutableList.of(
              testQuestionBank.applicantAddress().getQuestionDefinition(),
              testQuestionBank.applicantDate().getQuestionDefinition(),
              testQuestionBank.applicantKitchenTools().getQuestionDefinition()));

  @Before
  public void setUp() {
    formFactory = instanceOf(FormFactory.class);
    predicateGenerator = instanceOf(PredicateGenerator.class);
  }

  @Test
  public void generatePredicateDefinition_singleQuestion_singleValue() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "ZIP",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    PredicateDefinition predicateDefinition =
        predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat().get())
        .isEqualTo(PredicateDefinition.PredicateFormat.SINGLE_QUESTION);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.HIDE_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .isEqualTo(ImmutableList.of(testQuestionBank.applicantAddress().id));
    assertThat(predicateDefinition.rootNode())
        .isEqualTo(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.builder()
                    .setQuestionId(testQuestionBank.applicantAddress().id)
                    .setScalar(Scalar.ZIP)
                    .setOperator(Operator.EQUAL_TO)
                    .setComparedValue(PredicateValue.of("98144"))
                    .build()));
  }

  @Test
  public void generatePredicateDefinition_multiQuestion_multiValue() throws Exception {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "SHOW_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "ZIP",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144",
                String.format(
                    "group-2-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "10001",
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
        predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat().get())
        .isEqualTo(PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS);
    assertThat(predicateDefinition.action()).isEqualTo(PredicateAction.SHOW_BLOCK);
    assertThat(predicateDefinition.getQuestions())
        .containsExactlyInAnyOrder(
            testQuestionBank.applicantAddress().id, testQuestionBank.applicantDate().id);

    assertThat(predicateDefinition.rootNode().getType()).isEqualTo(PredicateExpressionNodeType.OR);
    assertThat(predicateDefinition.rootNode().getOrNode().children())
        .containsExactly(
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantDate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(stringToPredicateDate("2023-01-01"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantAddress().id)
                                .setScalar(Scalar.ZIP)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of("98144"))
                                .build())))),
            PredicateExpressionNode.create(
                AndNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantDate().id)
                                .setScalar(Scalar.DATE)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(stringToPredicateDate("2023-02-02"))
                                .build()),
                        PredicateExpressionNode.create(
                            LeafOperationExpressionNode.builder()
                                .setQuestionId(testQuestionBank.applicantAddress().id)
                                .setScalar(Scalar.ZIP)
                                .setOperator(Operator.EQUAL_TO)
                                .setComparedValue(PredicateValue.of("10001"))
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
        predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService);

    assertThat(predicateDefinition.predicateFormat().get())
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
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  @Test
  public void generatePredicateDefinition_invalidAction() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "invalid",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "ZIP",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    assertThatThrownBy(
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void generatePredicateDefinition_missingScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    assertThatThrownBy(
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void generatePredicateDefinition_invalidScalar() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "invalid",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "EQUAL_TO",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    assertThatThrownBy(
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
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
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    assertThatThrownBy(
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void generatePredicateDefinition_invalidOperator() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "predicateAction",
                "HIDE_BLOCK",
                String.format("question-%d-scalar", testQuestionBank.applicantAddress().id),
                "ZIP",
                String.format("question-%d-operator", testQuestionBank.applicantAddress().id),
                "invalid",
                String.format(
                    "group-1-question-%d-predicateValue", testQuestionBank.applicantAddress().id),
                "98144"));

    assertThatThrownBy(
            () -> predicateGenerator.generatePredicateDefinition(form, readOnlyQuestionService))
        .isInstanceOf(BadRequestException.class);
  }

  private PredicateValue stringToPredicateDate(String rawDate) {
    LocalDate localDate = LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    return PredicateValue.of(localDate);
  }

  private DynamicForm buildForm(ImmutableMap<String, String> formContents) {
    return formFactory.form().bindFromRequest(fakeRequest().bodyForm(formContents).build());
  }
}
