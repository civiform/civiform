package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.question.types.QuestionDefinition;

/**
 * Represents the boolean operator AND. Each of the child predicates must evaluate to true for the
 * entire AND node to be considered true.
 */
@JsonTypeName("and")
@AutoValue
public abstract class AndNode implements ConcretePredicateExpressionNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(AndNode.class);

  /**
   * Create a new AND node.
   *
   * @param children the child nodes of this AND node. Ordering is preserved.
   */
  @JsonCreator
  public static AndNode create(
      @JsonProperty("children") ImmutableList<PredicateExpressionNode> children) {
    return new AutoValue_AndNode(children);
  }

  /** The child nodes of this AND node. Ordering is stable. */
  @JsonProperty("children")
  public abstract ImmutableList<PredicateExpressionNode> children();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.AND;
  }

  @Override
  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    // Sorted to ensure consistent rendering. A question should always be
    // found for a child node but if it's not defaulting to Z prevents the
    // code from causing an exception.
    Comparator<PredicateExpressionNode> childComparator =
        Comparator.comparing(
            node ->
                node.getQuestions().stream()
                    .findFirst()
                    .flatMap(
                        qid ->
                            questions.stream()
                                .filter(question -> question.getId() == qid)
                                .findFirst())
                    .map(QuestionDefinition::getName)
                    .orElseGet(
                        () -> {
                          LOGGER.error(
                              "Question not found for node question IDs: {}, provided questions:"
                                  + " {}",
                              node.getQuestions(),
                              questions.stream().map(QuestionDefinition::getId));
                          return "Z";
                        }));

    return Joiner.on(" and ")
        .join(
            children().stream()
                .sorted(childComparator)
                .map(c -> c.node().toDisplayString(questions))
                .toArray());
  }
}
