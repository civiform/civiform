package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import services.question.types.QuestionDefinition;

/**
 * Conditional logic associated with a single program screen (block). This may be used to (for
 * example) show or hide a block based on the answers an applicant provides.
 *
 * <p>The class is annotated to ignore "predicateFormat" as a JSON property due to a previous
 * version of the code that stored the predicate format in the database rather than computing it
 * from the expression tree.
 */
@AutoValue
@JsonIgnoreProperties(value = {"predicateFormat"})
public abstract class PredicateDefinition {

  /** Indicates the shape of the predicate's AST so view code can render the appropriate UI. */
  public enum PredicateFormat {
    // A single leaf node.
    SINGLE_QUESTION,
    // A top level OR with only AND child nodes, each AND node's children are all leaf nodes.
    OR_OF_SINGLE_LAYER_ANDS;
  }

  @JsonCreator
  public static PredicateDefinition create(
      @JsonProperty("rootNode") PredicateExpressionNode rootNode,
      @JsonProperty("action") PredicateAction action) {
    return new AutoValue_PredicateDefinition(rootNode, action);
  }

  /** Determines what {@link PredicateFormat} a given predicate expression tree is. */
  public static PredicateFormat detectPredicateFormat(PredicateExpressionNode rootNode) {
    return switch (rootNode.getType()) {
      case OR -> PredicateFormat.OR_OF_SINGLE_LAYER_ANDS;
      case LEAF_ADDRESS_SERVICE_AREA, LEAF_OPERATION -> PredicateFormat.SINGLE_QUESTION;
      default ->
          throw new IllegalStateException(
              String.format("Unsupported predicate expression format: %s", rootNode));
    };
  }

  @JsonProperty("rootNode")
  public abstract PredicateExpressionNode rootNode();

  @JsonProperty("action")
  public abstract PredicateAction action();

  /**
   * Returns the question IDs referenced by this predicate, deduplicated but presented in a list to
   * preserve ordering.
   */
  @JsonIgnore
  @Memoized
  public ImmutableList<Long> getQuestions() {
    return rootNode().getQuestions().stream().distinct().collect(toImmutableList());
  }

  /** Indicates the shape of the predicate's AST so view code can render the appropriate UI. */
  public PredicateFormat predicateFormat() {
    return detectPredicateFormat(rootNode());
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
