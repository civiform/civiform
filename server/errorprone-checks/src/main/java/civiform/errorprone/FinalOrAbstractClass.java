package civiform.errorprone;

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.Modifier;

/**
 * Requires every class to be declared {@code final} or {@code abstract}, so that allowing
 * subclassing is an explicit decision rather than the default.
 *
 * <p>Interfaces, enums, records and annotations are ignored, as are anonymous classes and classes
 * declared {@code sealed} or {@code non-sealed}, since those already make the subclassing decision
 * explicit. Ebean entity classes are ignored because Ebean enhances them at build time, and so is
 * generated code.
 *
 * <p>A class that must be both instantiable and open for subclassing opts out with
 * {@code @SuppressWarnings("FinalOrAbstractClass")}.
 */
@BugPattern(
    summary =
        "Classes must be declared final or abstract. Without final, a class is open for"
            + " subclassing by default. If the class must be instantiable and subclassable, add"
            + " @SuppressWarnings(\"FinalOrAbstractClass\").",
    severity = ERROR,
    linkType = NONE)
public final class FinalOrAbstractClass extends BugChecker implements ClassTreeMatcher {

  private static final ImmutableSet<Modifier> EXPLICIT_SUBCLASSING_MODIFIERS =
      ImmutableSet.of(Modifier.FINAL, Modifier.ABSTRACT, Modifier.SEALED, Modifier.NON_SEALED);

  private static final ImmutableSet<String> PERSISTENCE_ANNOTATIONS =
      ImmutableSet.of(
          "jakarta.persistence.Entity",
          "jakarta.persistence.MappedSuperclass",
          "jakarta.persistence.Embeddable");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getKind() != Tree.Kind.CLASS || tree.getSimpleName().isEmpty()) {
      return NO_MATCH;
    }

    Set<Modifier> modifiers = tree.getModifiers().getFlags();
    if (!Collections.disjoint(modifiers, EXPLICIT_SUBCLASSING_MODIFIERS)) {
      return NO_MATCH;
    }

    if (PERSISTENCE_ANNOTATIONS.stream().anyMatch(a -> hasAnnotation(tree, a, state))
        || GeneratedSources.isGenerated(state)) {
      return NO_MATCH;
    }

    return SuggestedFixes.addModifiers(tree, state, Modifier.FINAL)
        .map(fix -> describeMatch(tree, fix))
        .orElseGet(() -> describeMatch(tree));
  }
}
