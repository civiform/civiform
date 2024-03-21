package services.openApi;

import java.util.ArrayList;
import java.util.List;
import services.question.types.QuestionDefinition;

/**
 * N-ary tree to hold QuestionDefinitions in a nested manner to make it easier to handle enumerators
 */
public final class QuestionDefinitionNode {
  private final QuestionDefinition questionDefinition;
  private final List<QuestionDefinitionNode> children = new ArrayList<>();

  public static QuestionDefinitionNode createRootNode() {
    return new QuestionDefinitionNode(null);
  }

  private QuestionDefinitionNode(QuestionDefinition questionDefinition) {
    this.questionDefinition = questionDefinition;
  }

  public QuestionDefinition getQuestionDefinition() {
    return questionDefinition;
  }

  public List<QuestionDefinitionNode> getChildren() {
    return children;
  }

  /**
   * Add the next question definition, if the question is part of an enumerator it will find the
   * appropriate location in the tree to place it
   */
  public void addQuestionDefinition(QuestionDefinition newQuestionDefinition) {
    // No enumerator id, just add it as a child don't bother searching for a parent
    if (newQuestionDefinition.getEnumeratorId().isEmpty()) {
      children.add(new QuestionDefinitionNode(newQuestionDefinition));
      return;
    }

    for (QuestionDefinitionNode child : children) {
      // Check if the questionDefinition to add has en existing parent enumerator
      // already in the tree; if it does add it
      if (newQuestionDefinition.getEnumeratorId().isPresent()
          && child.questionDefinition.getId() == newQuestionDefinition.getEnumeratorId().get()) {
        child.children.add(new QuestionDefinitionNode(newQuestionDefinition));
        return;
      }

      child.addQuestionDefinition(newQuestionDefinition);
    }
  }
}
