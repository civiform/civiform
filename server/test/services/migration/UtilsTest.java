package services.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static support.TestQuestionBank.createQuestionDefinition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
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
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public class UtilsTest {
  private static final Long PROGRAM_ID_1 = 1000L;
  private QuestionDefinition OLD_QUESTION_1;
  private QuestionDefinition OLD_QUESTION_2;
  private QuestionDefinition NEW_QUESTION_1;
  private QuestionDefinition NEW_QUESTION_2;

  @Before
  public void setup() {
    OLD_QUESTION_1 = createQuestionDefinition("question1", 1L, QuestionType.ADDRESS);
    OLD_QUESTION_2 = createQuestionDefinition("question2", 2L, QuestionType.TEXT);
    NEW_QUESTION_1 = createQuestionDefinition("question1", 101L, QuestionType.ADDRESS);
    NEW_QUESTION_2 = createQuestionDefinition("question2", 102L, QuestionType.TEXT);
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
}
