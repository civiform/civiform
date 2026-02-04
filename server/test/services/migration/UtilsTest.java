package services.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public class UtilsTest {
  private static final Long PROGRAM_ID_1 = 1000L;
  private QuestionDefinition OLD_QUESTION_1;
  private QuestionDefinition OLD_QUESTION_2;
  private QuestionDefinition NEW_QUESTION_1;
  private QuestionDefinition NEW_QUESTION_2;

  @Before
  public void setup() throws UnsupportedQuestionTypeException {
    OLD_QUESTION_1 = makeQuestion("question1", QuestionType.ADDRESS, 1L);
    OLD_QUESTION_2 = makeQuestion("question2", QuestionType.TEXT, 2L);
    NEW_QUESTION_1 = makeQuestion("question1", QuestionType.ADDRESS, 101L);
    NEW_QUESTION_2 = makeQuestion("question2", QuestionType.TEXT, 102L);
  }

  @Test
  public void convertNumberToSuffix_generatesCorrectSuffix() {
    assertThat(Utils.convertNumberToSuffix(10)).isEqualTo("j");
    assertThat(Utils.convertNumberToSuffix(26)).isEqualTo("z");
    assertThat(Utils.convertNumberToSuffix(26 + 2)).isEqualTo("ab");
    assertThat(Utils.convertNumberToSuffix(26 + 26)).isEqualTo("az");
    assertThat(Utils.convertNumberToSuffix(26 + 26 + 26)).isEqualTo("bz");
    assertThat(Utils.convertNumberToSuffix(26 * 26 + 11)).isEqualTo("zk");
    assertThat(Utils.convertNumberToSuffix(27 * 26 + 11)).isEqualTo("aak");
  }

  @Test
  public void updateBlockDefinitions_updatesQuestionDefinitions() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("block1")
            .withRequiredQuestionDefinition(OLD_QUESTION_1)
            .withRequiredQuestionDefinition(OLD_QUESTION_2)
            .withBlock("block2")
            .buildDefinition();
    // Create input and output question maps
    ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
        ImmutableMap.of(1L, OLD_QUESTION_1, 2L, OLD_QUESTION_2);
    ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
        ImmutableMap.of("question1", NEW_QUESTION_1, "question2", NEW_QUESTION_2);

    ImmutableList<BlockDefinition> result =
        Utils.updateBlockDefinitions(programDefinition, questionsOnJsonById, updatedQuestionsMap);

    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    BlockDefinition resultBlockDefinition1 = result.get(0);
    assertThat(resultBlockDefinition1.getQuestionCount()).isEqualTo(2);
    assertThat(resultBlockDefinition1.getQuestionDefinition(0)).isEqualTo(NEW_QUESTION_1);
    assertThat(resultBlockDefinition1.getQuestionDefinition(1)).isEqualTo(NEW_QUESTION_2);
    BlockDefinition resultBlockDefinition2 = result.get(1);
    assertThat(resultBlockDefinition2)
        .isEqualTo(programDefinition.getBlockDefinitionByIndex(1).get());
  }

  @Test
  public void updateBlockDefinitions_withEmptyBlockDefinitions_returnsEmptyList() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1).buildDefinition();
    // Create input and output question maps
    ImmutableMap<Long, QuestionDefinition> questionsOnJsonById = ImmutableMap.of();
    ImmutableMap<String, QuestionDefinition> updatedQuestionsMap = ImmutableMap.of();

    ImmutableList<BlockDefinition> result =
        Utils.updateBlockDefinitions(programDefinition, questionsOnJsonById, updatedQuestionsMap);

    // Verify results
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getQuestionCount()).isEqualTo(0);
  }

  @Test
  public void updateBlockDefinitions_withPredicates_updatesPredicates() {
    // Create leaf node for predicate
    PredicateExpressionNode predicateNode =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                /* questionId= */ OLD_QUESTION_1.getId(),
                Scalar.NUMBER,
                Operator.GREATER_THAN_OR_EQUAL_TO,
                PredicateValue.of(1000L)));
    PredicateDefinition predicateDefinition =
        PredicateDefinition.create(predicateNode, PredicateAction.SHOW_BLOCK);
    EligibilityDefinition eligibilityDefinition =
        EligibilityDefinition.builder().setPredicate(predicateDefinition).build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("block1")
            .withRequiredQuestionDefinition(OLD_QUESTION_1)
            .withVisibilityPredicate(predicateDefinition)
            .withEligibilityDefinition(eligibilityDefinition)
            .buildDefinition();
    // Create input and output question maps
    ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
        ImmutableMap.of(1L, OLD_QUESTION_1);
    ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
        ImmutableMap.of("question1", NEW_QUESTION_1);

    ImmutableList<BlockDefinition> result =
        Utils.updateBlockDefinitions(programDefinition, questionsOnJsonById, updatedQuestionsMap);

    // Verify results
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    BlockDefinition resultBlockDefinition = result.get(0);
    assertThat(resultBlockDefinition.getQuestionCount()).isEqualTo(1);
    assertThat(resultBlockDefinition.getQuestionDefinition(0)).isEqualTo(NEW_QUESTION_1);
    assertThat(resultBlockDefinition.visibilityPredicate()).isPresent();
    PredicateDefinition resultPredicate = resultBlockDefinition.visibilityPredicate().get();
    assertThat(resultPredicate.action()).isEqualTo(predicateDefinition.action());
    assertThat(resultPredicate.predicateFormat()).isEqualTo(predicateDefinition.predicateFormat());
    assertThat(resultPredicate.getQuestions()).isEqualTo(ImmutableList.of(NEW_QUESTION_1.getId()));
    assertThat(resultPredicate.rootNode().getLeafNode())
        .isEqualTo(
            LeafOperationExpressionNode.create(
                /* questionId= */ NEW_QUESTION_1.getId(),
                Scalar.NUMBER,
                Operator.GREATER_THAN_OR_EQUAL_TO,
                PredicateValue.of(1000L)));
  }

  private static QuestionDefinition makeQuestion(String name, QuestionType type, Long id)
      throws UnsupportedQuestionTypeException {
    return new QuestionDefinitionBuilder()
        .setName(name)
        .setId(id)
        .setQuestionType(type)
        .setQuestionText(LocalizedStrings.withDefaultValue(name))
        .setDescription(name)
        .build();
  }
}
