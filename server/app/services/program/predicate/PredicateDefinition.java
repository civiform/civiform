package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.question.types.QuestionDefinition;

/**
 * Conditional logic associated with a single program screen (block). This may be used to (for
 * example) show or hide a block based on the answers an applicant provides.
 */
@AutoValue
public abstract class PredicateDefinition {

  /** Indicates the shape of the predicate's AST so view code can render the appropriate UI. */
  public enum PredicateFormat {
    // A single leaf node.
    SINGLE_QUESTION,
    // A top level OR with only AND child nodes,
    // each AND node's children are all leaf nodes.
    OR_OF_SINGLE_LAYER_ANDS;
  }

  @JsonCreator
  public static PredicateDefinition create(
      @JsonProperty("rootNode") PredicateExpressionNode rootNode,
      @JsonProperty("action") PredicateAction action,
      @JsonProperty("predicateFormat") Optional<PredicateFormat> predicateFormat) {
    return new AutoValue_PredicateDefinition(rootNode, action, predicateFormat);
  }

  public static PredicateDefinition create(
      PredicateExpressionNode rootNode, PredicateAction action, PredicateFormat predicateFormat) {
    return create(rootNode, action, Optional.of(predicateFormat));
  }

  public static PredicateDefinition create(
      PredicateExpressionNode rootNode, PredicateAction action) {
    return create(rootNode, action, /* predicateFormat= */ Optional.empty());
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
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonProperty("predicateFormat")
  public abstract Optional<PredicateFormat> predicateFormat();

  /**
   * Returns the {@code predicateFormat} if present otherwise defaults to SINGLE_QUESTION.
   *
   * <p>Before {@link PredicateFormat} was added, all predicates were a single leaf node.
   * PredicateDefinitions saved without formats are therefore assumed to have that shape.
   */
  public PredicateFormat computePredicateFormat() {
    return predicateFormat().orElse(PredicateFormat.SINGLE_QUESTION);
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
