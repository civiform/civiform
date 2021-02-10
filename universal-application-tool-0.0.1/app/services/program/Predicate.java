

/**
 * String representation of a predicate with variables from the core data model.
 */
@AutoValue
abstract class Predicate {
    static Predicate create(String function, Set<String> variables) {
        return new AutoValue_Predicate(function, variables);
    }

    abstract String function();

    abstract ImmutableSet<String> variables();
}