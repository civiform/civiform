package services.program.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import j2html.tags.UnescapedText;
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

  private static final Logger logger = LoggerFactory.getLogger(AndNode.class);

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
  @JsonIgnore
  public void accept(PredicateExpressionNodeVisitor visitor) {
    children().stream().forEach(child -> child.accept(visitor));
    visitor.visit(this);
  }

  @Override
  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    Comparator<PredicateExpressionNode> childComparator = getChildComparator(questions);
    return Joiner.on(" AND ")
        .join(
            children().stream()
                .sorted(childComparator)
                .map(c -> c.node().toDisplayString(questions))
                .toArray());
  }

  @Override
  public UnescapedText toDisplayFormattedHtml(ImmutableList<QuestionDefinition> questions) {
    Comparator<PredicateExpressionNode> childComparator = getChildComparator(questions);
    ImmutableList<UnescapedText> sortedQuestions =
        children().stream()
            .sorted(childComparator)
            .map(c -> c.node().toDisplayFormattedHtml(questions))
            .collect(ImmutableList.toImmutableList());
    return PredicateUtils.joinUnescapedText(sortedQuestions, /* delimiter= */ "AND");
  }

  /**
   * Sorts questions to ensure consistent rendering. A question should always be found for a child
   * node but if it's not, default to 'Z' to place its sort order toward the end.
   */
  Comparator<PredicateExpressionNode> getChildComparator(
      ImmutableList<QuestionDefinition> questions) {
    return Comparator.comparing(
        node ->
            node.getQuestions().stream()
                .findFirst()
                .flatMap(
                    qid ->
                        questions.stream().filter(question -> question.getId() == qid).findFirst())
                .map(QuestionDefinition::getName)
                .orElseGet(
                    () -> {
                      logger.error(
                          "Question not found for node question IDs: {}, provided questions:"
                              + " {}",
                          node.getQuestions(),
                          questions.stream().map(QuestionDefinition::getId));
                      return "Z";
                    }));
  }
}
