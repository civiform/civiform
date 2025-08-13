package services.program.predicate;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import j2html.tags.UnescapedText;
import services.question.types.QuestionDefinition;

/**
 * Represents the boolean operator OR. At least one of the child predicates must evaluate to true
 * for the entire OR tree to be considered true.
 */
@JsonTypeName("or")
@AutoValue
public abstract class OrNode implements ConcretePredicateExpressionNode {

  /**
   * Create a new OR node.
   *
   * @param children the child nodes of this OR node. Ordering is preserved.
   */
  @JsonCreator
  public static OrNode create(
      @JsonProperty("children") ImmutableList<PredicateExpressionNode> children) {
    return new AutoValue_OrNode(children);
  }

  /** The child nodes of this OR node. Ordering is stable. */
  @JsonProperty("children")
  public abstract ImmutableList<PredicateExpressionNode> children();

  @Override
  @JsonIgnore
  public PredicateExpressionNodeType getType() {
    return PredicateExpressionNodeType.OR;
  }

  @Override
  @JsonIgnore
  public void accept(PredicateExpressionNodeVisitor visitor) {
    children().stream().forEach(child -> child.accept(visitor));
    visitor.visit(this);
  }

  @Override
  public String toDisplayString(ImmutableList<QuestionDefinition> questions) {
    return Joiner.on(" OR ")
        .join(children().stream().map(c -> c.node().toDisplayString(questions)).toArray());
  }

  @Override
  public UnescapedText toDisplayFormattedHtml(ImmutableList<QuestionDefinition> questions) {
    return PredicateUtils.joinUnescapedText(
        children().stream()
            .map(c -> c.node().toDisplayFormattedHtml(questions))
            .collect(toImmutableList()),
        /* delimiter= */ "OR");
  }
}
