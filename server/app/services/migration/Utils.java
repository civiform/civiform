package services.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.ProgramMigrationWrapper;
import java.util.Map;
import java.util.Optional;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;

/**
 * Utility class for interacting with program & question definitions during migraiton. All methods
 * should be static.
 */
final class Utils {
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

  /**
   * Convert a number to the equivalent "excel column name".
   *
   * <p>For example, 5 maps to "e", and 28 maps to "ab".
   *
   * @param num to convert
   * @return The "excel column name" form of the number
   */
  static String convertNumberToSuffix(int num) {
    String result = "";

    // Division algorithm to convert from base 10 to "base 26"
    int dividend = num; // 28
    while (dividend > 0) {
      // Subtract one so we're doing math with a zero-based index.
      // We need "a" to be 0, and "z" to be 25, so that 26 wraps around
      // to be "aa". "a" is "ten" in base 26.
      dividend = dividend - 1;
      int remainder = dividend % 26;
      result = ALPHABET.charAt(remainder) + result;
      dividend = dividend / 26;
      ;
    }

    return result;
  }

  /**
   * Update the block definitions on the program with the newly saved questions before saving the
   * program.
   */
  static ImmutableList<BlockDefinition> updateBlockDefinitions(
      ProgramDefinition programOnJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return programOnJson.blockDefinitions().stream()
        .map(
            blockDefinition -> {
              ImmutableList<ProgramQuestionDefinition> updatedProgramQuestionDefinitions =
                  blockDefinition.programQuestionDefinitions().stream()
                      .map(
                          pqd ->
                              updateProgramQuestionDefinition(
                                  pqd, questionsOnJsonById, updatedQuestionsMap))
                      .collect(ImmutableList.toImmutableList());

              BlockDefinition.Builder blockDefinitionBuilder =
                  maybeUpdatePredicates(blockDefinition, questionsOnJsonById, updatedQuestionsMap);
              blockDefinitionBuilder.setProgramQuestionDefinitions(
                  updatedProgramQuestionDefinitions);

              return blockDefinitionBuilder.build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Map through each imported question and create a new ProgramQuestionDefinition to save on the
   * program. We use the old question id from the json to fetch the question admin name and match it
   * to the newly saved question so we can create a ProgramQuestionDefinition with the updated
   * question.
   */
  static ProgramQuestionDefinition updateProgramQuestionDefinition(
      ProgramQuestionDefinition programQuestionDefinitionFromJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    Long id = programQuestionDefinitionFromJson.id();
    String adminName = questionsOnJsonById.get(id).getName();
    QuestionDefinition updatedQuestion = updatedQuestionsMap.get(adminName);
    return ProgramQuestionDefinition.create(
        updatedQuestion,
        Optional.empty(),
        programQuestionDefinitionFromJson.optional(),
        programQuestionDefinitionFromJson.addressCorrectionEnabled());
  }

  /**
   * If there are eligibility and/or visibility predicates on the questions, update those with the
   * id of the newly saved question.
   */
  static BlockDefinition.Builder maybeUpdatePredicates(
      BlockDefinition blockDefinition,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    BlockDefinition.Builder blockDefinitionBuilder = blockDefinition.toBuilder();

    if (blockDefinition.visibilityPredicate().isPresent()) {
      PredicateDefinition visibilityPredicateDefinition =
          blockDefinition.visibilityPredicate().get();
      PredicateDefinition newPredicateDefinition =
          updatePredicateDefinition(
              visibilityPredicateDefinition, questionsOnJsonById, updatedQuestionsMap);
      blockDefinitionBuilder.setVisibilityPredicate(newPredicateDefinition);
    }
    if (blockDefinition.eligibilityDefinition().isPresent()) {
      PredicateDefinition eligibilityPredicateDefinition =
          blockDefinition.eligibilityDefinition().get().predicate();
      PredicateDefinition newPredicateDefinition =
          updatePredicateDefinition(
              eligibilityPredicateDefinition, questionsOnJsonById, updatedQuestionsMap);
      EligibilityDefinition newEligibilityDefinition =
          EligibilityDefinition.builder().setPredicate(newPredicateDefinition).build();
      blockDefinitionBuilder.setEligibilityDefinition(newEligibilityDefinition);
    }
    return blockDefinitionBuilder;
  }

  /**
   * Update the predicate definition by updating the predicate expression, starting with the root
   * node.
   */
  static PredicateDefinition updatePredicateDefinition(
      PredicateDefinition predicateDefinition,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return PredicateDefinition.create(
        updatePredicateExpression(
            predicateDefinition.rootNode(), questionsOnJsonById, updatedQuestionsMap),
        predicateDefinition.action());
  }

  /**
   * Update the eligibility or visibility predicate with the id from the newly saved question. We
   * use the old question id from the json to fetch the question admin name and match it to the
   * newly saved question so we can set the new question id on the predicate definition. We
   * recursively call this function on predicates with children.
   */
  static PredicateExpressionNode updatePredicateExpression(
      PredicateExpressionNode predicateExpressionNode,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {

    return switch (predicateExpressionNode.getType()) {
      case OR -> {
        OrNode orNode = predicateExpressionNode.getOrNode();
        ImmutableList<PredicateExpressionNode> orNodeChildren =
            orNode.children().stream()
                .map(
                    child ->
                        updatePredicateExpression(child, questionsOnJsonById, updatedQuestionsMap))
                .collect(ImmutableList.toImmutableList());
        yield PredicateExpressionNode.create(OrNode.create(orNodeChildren));
      }
      case AND -> {
        AndNode andNode = predicateExpressionNode.getAndNode();
        ImmutableList<PredicateExpressionNode> andNodeChildren =
            andNode.children().stream()
                .map(
                    child ->
                        updatePredicateExpression(child, questionsOnJsonById, updatedQuestionsMap))
                .collect(ImmutableList.toImmutableList());
        yield PredicateExpressionNode.create(AndNode.create(andNodeChildren));
      }
      case LEAF_ADDRESS_SERVICE_AREA -> {
        LeafAddressServiceAreaExpressionNode leafAddressNode =
            predicateExpressionNode.getLeafAddressNode();
        yield PredicateExpressionNode.create(
            leafAddressNode.toBuilder()
                .setQuestionId(
                    getNewQuestionid(leafAddressNode, questionsOnJsonById, updatedQuestionsMap))
                .build());
      }
      case LEAF_OPERATION -> {
        LeafOperationExpressionNode leafNode = predicateExpressionNode.getLeafOperationNode();
        yield PredicateExpressionNode.create(
            leafNode.toBuilder()
                .setQuestionId(getNewQuestionid(leafNode, questionsOnJsonById, updatedQuestionsMap))
                .build());
      }
    };
  }

  static Long getNewQuestionid(
      LeafExpressionNode leafNode,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    Long oldQuestionId = leafNode.questionId();
    String questionAdminName = questionsOnJsonById.get(oldQuestionId).getName();
    return updatedQuestionsMap.get(questionAdminName).getId();
  }

  static ImmutableList<String> getQuestionNamesForDuplicateHandling(
      ImmutableMap<String, ProgramMigrationWrapper.DuplicateQuestionHandlingOption>
          duplicateHandlingPerQuestion,
      ProgramMigrationWrapper.DuplicateQuestionHandlingOption duplicateHandling) {
    return duplicateHandlingPerQuestion.entrySet().stream()
        .filter(entry -> entry.getValue() == duplicateHandling)
        .map(Map.Entry::getKey)
        .collect(ImmutableList.toImmutableList());
  }
}
