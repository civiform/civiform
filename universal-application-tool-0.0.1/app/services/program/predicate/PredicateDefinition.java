package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import services.question.types.QuestionDefinition;

/**
 * Conditional logic associated with a single program screen (block). This may be used to (for
 * example) show or hide a block based on the answers an applicant provides.
 */
@AutoValue
public abstract class PredicateDefinition {

  @JsonCreator
  public static PredicateDefinition create(
      @JsonProperty("rootNode") PredicateExpressionNode rootNode,
      @JsonProperty("action") PredicateAction action) {
    return new AutoValue_PredicateDefinition(rootNode, action);
  }

  @JsonProperty("rootNode")
  public abstract PredicateExpressionNode rootNode();

  @JsonProperty("action")
  public abstract PredicateAction action();

  @JsonIgnore
  @Memoized
  public ImmutableSet<Long> getQuestions() {
    return rootNode().getQuestions();
  }

  /**
   * Formats this predicate definition as a human-readable sentence, in the format "[block name] is
   * [hidden or shown if] [predicate expression]" - ex: "My Block is hidden if applicant address's
   * city is equal to 'Seattle'".
   */
  public String toDisplayString(String blockName, ImmutableList<QuestionDefinition> questions) {
    return Joiner.on(' ')
        .join(blockName, "is", action().toDisplayString(), rootNode().toDisplayString(questions));
  }
}
