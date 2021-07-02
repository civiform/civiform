package services.program.predicate;

/**
 * An enum defining the types of nodes supported in a predicate expression. A predicate expression
 * is represented as a tree structure. A terminal node can be evaluated by itself while a
 * non-terminal node is evaluated based on the results of its children.
 */
public enum PredicateExpressionNodeType {
  AND,
  OR,
  LEAF_OPERATION
}
